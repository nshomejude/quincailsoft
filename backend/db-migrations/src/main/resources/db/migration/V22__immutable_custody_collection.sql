-- Collection proof documents custody transfer and must remain append-only.
CREATE TRIGGER custody_collection_immutable
    BEFORE UPDATE OR DELETE ON custody_collection
    FOR EACH ROW EXECUTE FUNCTION reject_fact_mutation();
