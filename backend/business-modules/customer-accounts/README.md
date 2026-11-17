# Customer-account migration boundary

A scoped JPA read API validates that a customer exists and is active in the authenticated tenant/legal company. Sales uses this public API instead of accessing customer tables. The current foundation does not yet implement customer onboarding/editing, statements, credit limits, deposits, allocations or collections. Synthetic acceptance fixtures are created directly only by test setup.
