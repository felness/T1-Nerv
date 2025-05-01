db = db.getSiblingDB('documents');

db.createUser({
    user: "user",
    pwd: "pass",
    roles: [
        { role: "readWrite", db: "documents" },
        { role: "dbAdmin", db: "documents" }
    ]
});

db.createCollection('init');
