package net.tropicraft.core.common.dimension.surfacebuilders;

public class TropicsSurfaceBuilder { //extends SurfaceBuilder<TropicsSurfaceBuilder.Config> {
//    public TropicsSurfaceBuilder(Codec<Config> codec) {
//        super(codec);
//    }
//
//    @Override
//    public void apply(Random random, ChunkAccess chunk, Biome biome, int x, int z, int startHeight, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel, int minSurfaceLevel, long seed, Config config) {
//        SurfaceBuilderBaseConfiguration selectedConfig = config.land;
//        if (noise > 1.5) {
//            if (chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) + 1 >= seaLevel) {
//                selectedConfig = config.sandy;
//            } else {
//                selectedConfig = config.sandyUnderwater;
//            }
//        }
//
//        SurfaceBuilder.DEFAULT.apply(random, chunk, biome, x, z, startHeight, noise, defaultBlock, defaultFluid, seaLevel, minSurfaceLevel, seed, selectedConfig);
//    }
//
//    public static final class Config implements SurfaceBuilderConfiguration {
//        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> {
//            return instance.group(
//                    SurfaceBuilderBaseConfiguration.CODEC.fieldOf("land").forGetter(c -> c.land),
//                    SurfaceBuilderBaseConfiguration.CODEC.fieldOf("sandy").forGetter(c -> c.sandy),
//                    SurfaceBuilderBaseConfiguration.CODEC.fieldOf("sandy_underwater").forGetter(c -> c.sandyUnderwater)
//            ).apply(instance, Config::new);
//        });
//
//        public final SurfaceBuilderBaseConfiguration land;
//        public final SurfaceBuilderBaseConfiguration sandy;
//        public final SurfaceBuilderBaseConfiguration sandyUnderwater;
//
//        public Config(SurfaceBuilderBaseConfiguration land, SurfaceBuilderBaseConfiguration sandy, SurfaceBuilderBaseConfiguration sandyUnderwater) {
//            this.land = land;
//            this.sandy = sandy;
//            this.sandyUnderwater = sandyUnderwater;
//        }
//
//        @Override
//        public BlockState getTopMaterial() {
//            return this.land.getTopMaterial();
//        }
//
//        @Override
//        public BlockState getUnderMaterial() {
//            return this.land.getUnderMaterial();
//        }
//
//        @Override
//        public BlockState getUnderwaterMaterial() {
//            return this.sandyUnderwater.getUnderwaterMaterial();
//        }
//    }
}
