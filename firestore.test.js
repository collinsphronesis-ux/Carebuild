const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCasePayload(creatorId, overrides = {}) {
  const ts = new Date(Date.now() - 5000);
  return {
    creatorId,
    beneficiaryName: "Amina K.",
    region: "Nakuru, Kenya",
    title: "Post-Surgery Clinic Recovery & Nutrition",
    story: "Amina is recovering from emergency surgery and needs support for follow-up clinic visits and staple food for her two children.",
    allocationPlan: "60% clinic medication & bandages, 40% family food staples",
    category: "medical",
    supportType: "both",
    goalAmountCents: 25000,
    raisedAmountCents: 0,
    supporterCount: 0,
    repaymentMonths: 6,
    urgency: "urgent",
    status: "active",
    visibility: "public",
    coverAssetKey: "medical",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("beneficiaryCases").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("beneficiaryCases").doc("case_1").set(validCasePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("pledges").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("oreNotes").get());
});

test("BeneficiaryCase: owner create, public filtered query, and anti-spoofing guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid create
  await assertSucceeds(
    aliceDb.collection("beneficiaryCases").doc("case_1").set(validCasePayload(ALICE_UID))
  );

  // Filtered query matching Kotlin repository succeeds
  await assertSucceeds(
    bobDb.collection("beneficiaryCases").where("visibility", "==", "public").get()
  );

  // Unfiltered query on top-level collection fails
  await assertFails(bobDb.collection("beneficiaryCases").get());

  // Reject identity spoofing
  await assertFails(
    bobDb.collection("beneficiaryCases").doc("case_spoof").set(validCasePayload(ALICE_UID))
  );

  // Reject pre-inflated raisedAmountCents or extra @DocumentId 'id' field
  await assertFails(
    aliceDb.collection("beneficiaryCases").doc("case_fake_raised").set(
      validCasePayload(ALICE_UID, { raisedAmountCents: 5000 })
    )
  );
  await assertFails(
    aliceDb.collection("beneficiaryCases").doc("case_extra_id").set(
      validCasePayload(ALICE_UID, { id: "case_extra_id" })
    )
  );

  // Reject future tampered timestamp
  await assertFails(
    aliceDb.collection("beneficiaryCases").doc("case_future_ts").set(
      validCasePayload(ALICE_UID, { createdAt: new Date(Date.now() + 120000) })
    )
  );
});

test("BeneficiaryCase & Pledge: atomic supporter pledge and creator-only edits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("beneficiaryCases").doc("case_1").set(validCasePayload(ALICE_UID))
  );

  // Bob cannot edit Alice's story
  await assertFails(
    bobDb.collection("beneficiaryCases").doc("case_1").update({
      title: "Hijacked Title",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob CAN contribute a valid pledge and increment case progress atomically
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    bobDb.collection("beneficiaryCases").doc("case_1").update({
      raisedAmountCents: 5000,
      supporterCount: 1,
      status: "active",
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("pledges").doc("pledge_1").set({
      userId: BOB_UID,
      caseId: "case_1",
      caseTitle: "Post-Surgery Clinic Recovery & Nutrition",
      beneficiaryName: "Amina K.",
      supportType: "microloan",
      amountCents: 5000,
      repaidCents: 0,
      repaymentMonths: 6,
      note: "Wishing you a steady recovery.",
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice cannot read Bob's private pledge
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("pledges").doc("pledge_1").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("pledges").get());

  // Bob can query and update repayment on his own pledge
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).collection("pledges").get());
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("pledges").doc("pledge_1").update({
      repaidCents: 2500,
      status: "active",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot inflate repaidCents above amountCents
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).collection("pledges").doc("pledge_1").update({
      repaidCents: 99999,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("ImpactUpdates & OreNotes: creator-only updates and user-scoped Ore notes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("beneficiaryCases").doc("case_1").set(validCasePayload(ALICE_UID))
  );

  // Creator Alice can post an impact update
  await assertSucceeds(
    aliceDb.collection("beneficiaryCases").doc("case_1").collection("updates").doc("upd_1").set({
      caseId: "case_1",
      authorId: ALICE_UID,
      headline: "Clinic checkup completed",
      message: "First follow-up visit went smoothly and medication is covered for the month.",
      milestoneType: "medical_update",
      visibility: "public",
      createdAt: now,
    })
  );

  // Non-creator Bob cannot post an impact update on Alice's case
  await assertFails(
    bobDb.collection("beneficiaryCases").doc("case_1").collection("updates").doc("upd_2").set({
      caseId: "case_1",
      authorId: BOB_UID,
      headline: "Fake update",
      message: "Unauthorized update injection attempt.",
      milestoneType: "progress",
      visibility: "public",
      createdAt: now,
    })
  );

  // OreNotes isolated to owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("oreNotes").doc("note_1").set({
      userId: ALICE_UID,
      topic: "Weekly Impact Digest",
      prompt: "Summarize my active sponsorships",
      summary: "You are following 1 active medical case in Nakuru.",
      scopeType: "portfolio",
      createdAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("oreNotes").get());
});
