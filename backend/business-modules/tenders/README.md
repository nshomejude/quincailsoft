# Cash, customer receipts and deposits

Java25/JPA module with exact FCFA amounts, current scoped membership, row locks, immutable cash facts and durable operation replay. Public interfaces are in `api`, commands in `application`, and typed persistence in `infrastructure`. See `docs/migration/m03.md` for implemented commands, API routes and unresolved release gates. Opening floats are declared; they do not create sales or revenue.

`TenderPostgresTest` executes the actual services against fresh PostgreSQL18 schema using a non-owner NOBYPASSRLS runtime. Run with `QUINCAI_PG_TEST_URL`/`QUINCAI_PG_TEST_USER`; absent configuration is an explicit JUnit skip, never an integration pass.
