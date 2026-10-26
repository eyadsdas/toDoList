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

// 1. Unauthenticated checks
test("Unauthenticated user: cannot read task_lists", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("task_lists").get());
});

test("Unauthenticated user: cannot create task", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("tasks").doc("task1").set({
      listId: "list1",
      title: "Task 1",
      completed: false,
      creatorId: "unauth",
      memberUids: ["unauth"],
      createdAt: new Date(),
    })
  );
});

// 2. Authenticated user creating task list
test("Authenticated user: can create a valid task list where ownerId matches uid", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("task_lists").doc("list1").set({
      title: "Alice Work List",
      description: "My personal tasks",
      color: "#6750A4",
      ownerId: ALICE_UID,
      ownerName: "Alice",
      ownerEmail: "alice@example.com",
      memberUids: [ALICE_UID],
      memberEmails: ["alice@example.com"],
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create task list where ownerId does not match uid", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("task_lists").doc("list1").set({
      title: "Spoofed List",
      ownerId: BOB_UID,
      ownerName: "Bob",
      memberUids: [BOB_UID],
      createdAt: new Date(),
    })
  );
});

// 3. Cross-user isolation
test("Cross-user: Bob cannot read Alice's private task list", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("task_lists").doc("alice_list").set({
      title: "Alice's Secret List",
      ownerId: ALICE_UID,
      ownerName: "Alice",
      memberUids: [ALICE_UID],
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("task_lists").doc("alice_list").get());
});

// 4. Collaborative access
test("Collaborative: Bob can read task list when added to memberUids", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("task_lists").doc("shared_list").set({
      title: "Shared Team List",
      ownerId: ALICE_UID,
      ownerName: "Alice",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
    await context.firestore().collection("tasks").doc("task_shared").set({
      listId: "shared_list",
      title: "Finish Report",
      completed: false,
      creatorId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("task_lists").doc("shared_list").get());
  await assertSucceeds(bobDb.collection("tasks").doc("task_shared").get());
});

test("Query alignment: Alice queries tasks with memberUids array-contains", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("task_alice").set({
      listId: "list_a",
      title: "Design Specs",
      completed: false,
      creatorId: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").where("memberUids", "array-contains", ALICE_UID).get()
  );
});
