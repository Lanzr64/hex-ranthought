package com.lanzr.hexranthought.casting;

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import com.lanzr.hexranthought.items.FidgetItem;
import java.util.List;

public final class OrchardAction implements ConstMediaAction {
    private final boolean asList;
    public OrchardAction(boolean asList) { this.asList = asList; }
    @Override public int getArgc() { return 0; }
    @Override public long getMediaCost() { return 0; }

    @Override public ConstMediaAction.CostMediaActionResult executeWithOpCount(List<? extends Iota> args, CastingEnvironment env) {
        return ConstMediaAction.DefaultImpls.executeWithOpCount(this, args, env);
    }

    @Override public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) {
        return ConstMediaAction.DefaultImpls.operate(this, env, image, continuation);
    }

    @Override public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
        double value = env.getCaster() == null ? 0 : FidgetItem.getOrchard(env.getCaster());
        Iota datum = new DoubleIota(value);
        return asList ? List.of(new ListIota(List.of(datum))) : List.of(datum);
    }
}
