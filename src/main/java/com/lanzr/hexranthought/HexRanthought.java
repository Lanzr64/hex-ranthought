package com.lanzr.hexranthought;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.common.casting.actions.spells.OpMakePackagedSpell;
import at.petrak.hexcasting.common.lib.hex.HexActions;
import com.lanzr.hexranthought.blocks.AmethystGeneratorBlock;
import com.lanzr.hexranthought.blocks.AmethystGeneratorBlockEntity;
import com.lanzr.hexranthought.casting.GloopifactAction;
import com.lanzr.hexranthought.casting.OrchardAction;
import com.lanzr.hexranthought.casting.GloopimindAction;
import com.lanzr.hexranthought.casting.SwapEntitiesAction;
import com.lanzr.hexranthought.casting.SwapEffectsAction;
import com.lanzr.hexranthought.casting.KillPhantomAction;
import com.lanzr.hexranthought.casting.ThaumicShockAction;
import com.lanzr.hexranthought.casting.SpatialBladeAction;
import com.lanzr.hexranthought.items.FidgetItem;
import com.lanzr.hexranthought.items.DreidelFidgetItem;
import com.lanzr.hexranthought.items.GloopifactItem;
import com.lanzr.hexranthought.items.LibraryCardItem;
import com.lanzr.hexranthought.network.FidgetNetwork;
import com.lanzr.hexranthought.network.ArcNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod(HexRanthought.ID)
public final class HexRanthought {
    public static final String ID = "hex_ranthought";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final RegistryObject<GloopifactItem> GLOOPIFACT = ITEMS.register("gloopifact",
            () -> new GloopifactItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<FidgetItem> COPPER_PEN_FIDGET = ITEMS.register("copper_pen_fidget",
            () -> new FidgetItem(new Item.Properties().stacksTo(1), 2));
    public static final RegistryObject<DreidelFidgetItem> DREIDEL_FIDGET = ITEMS.register("dreidel_fidget",
            () -> new DreidelFidgetItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<FidgetItem> RAINBOW_AMOGUS_FIDGET = ITEMS.register("rainbow_amogus_fidget",
            () -> new FidgetItem(new Item.Properties().stacksTo(1), 6));
    public static final RegistryObject<LibraryCardItem> LIBRARY_CARD = ITEMS.register("library_card",
            () -> new LibraryCardItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<AmethystGeneratorBlock> AMETHYST_GENERATOR = BLOCKS.register("amethyst_generator",
            () -> new AmethystGeneratorBlock(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                    .requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> AMETHYST_GENERATOR_ITEM = ITEMS.register("amethyst_generator",
            () -> new BlockItem(AMETHYST_GENERATOR.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<AmethystGeneratorBlockEntity>> AMETHYST_GENERATOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("amethyst_generator", () -> BlockEntityType.Builder.of(
                    AmethystGeneratorBlockEntity::new, AMETHYST_GENERATOR.get()).build(null));
    private static final RegistryObject<CreativeModeTab> TAB = TABS.register("general",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.hex_ranthought.general"))
                    .icon(() -> GLOOPIFACT.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        out.accept(GLOOPIFACT.get());
                        out.accept(COPPER_PEN_FIDGET.get());
                        out.accept(DREIDEL_FIDGET.get());
                        out.accept(RAINBOW_AMOGUS_FIDGET.get());
                        out.accept(LIBRARY_CARD.get());
                        out.accept(AMETHYST_GENERATOR_ITEM.get());
                    }).build());

    public HexRanthought() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        bus.addListener(this::registerActions);
        FidgetNetwork.init();
        ArcNetwork.init();
        MinecraftForge.EVENT_BUS.addListener(this::copyOrchardOnRespawn);
    }

    private void copyOrchardOnRespawn(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().putDouble(FidgetItem.ORCHARD_TAG,
                event.getOriginal().getPersistentData().getDouble(FidgetItem.ORCHARD_TAG));
    }

    private void registerActions(RegisterEvent event) {
        if (!event.getRegistryKey().equals(HexActions.REGISTRY.key())) return;
        action(event, "gloopifact_read", "aqqqqqeqadaqw", HexDir.NORTH_EAST, new GloopifactAction(false, false));
        action(event, "gloopifact_write", "deeeeeqqadaqw", HexDir.NORTH_EAST, new GloopifactAction(true, false));
        action(event, "gloopifact_check_read", "wwqadaqwwaqqqqq", HexDir.NORTH_EAST, new GloopifactAction(false, true));
        action(event, "gloopifact_check_write", "wwqadaqwwdeeeee", HexDir.NORTH_EAST, new GloopifactAction(true, true));
        action(event, "craft/gloopifact", "wwaadaqwaweqqwaweewawqwwwwadeeeeeqww", HexDir.EAST,
                new OpMakePackagedSpell(GLOOPIFACT.get(), MediaConstants.CRYSTAL_UNIT * 16));
        action(event, "gloopimind_upload", "qewdweeddw", HexDir.NORTH_EAST, new GloopimindAction(true));
        action(event, "gloopimind_download", "qedadewdde", HexDir.NORTH_EAST, new GloopimindAction(false));
        action(event, "read_orchard", "dqqqqq", HexDir.SOUTH_EAST, new OrchardAction(false));
        action(event, "read_orchard_list", "dqqqqqdeeeqdqeee", HexDir.SOUTH_EAST, new OrchardAction(true));
        action(event, "swap_entities", "dwdeawdqawa", HexDir.EAST, new SwapEntitiesAction());
        action(event, "swap_effects", "waqdwaedw", HexDir.EAST, new SwapEffectsAction());
        action(event, "phantom_chicken", "qedaeawadeqaqedawaeqdq", HexDir.NORTH_EAST,
                new KillPhantomAction());
        action(event, "thaumic_shock", "adawqadewdad", HexDir.NORTH_EAST,
                new ThaumicShockAction());
        // NBT: start_dir 0; angles [0, 1, 5, 5, 5, 5, 5, 4, 0, 0, 0].
        action(event, "spatial_blade", "weqqqqqawww", HexDir.NORTH_EAST,
                new SpatialBladeAction());
    }

    private static void action(RegisterEvent event, String name, String angles, HexDir dir,
                               at.petrak.hexcasting.api.casting.castables.Action action) {
        event.register(HexActions.REGISTRY.key(), new ResourceLocation(ID, name),
                () -> new ActionRegistryEntry(HexPattern.fromAngles(angles, dir), action));
    }
}
