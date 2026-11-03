package com.deepspace.compat.mixin;

/** Access bridge for the vanilla structure pool's mutable raw-template list. */
public interface StructureTemplatePoolRawTemplatesAccess {
    void deepspace$makeRawTemplatesMutable();
}
