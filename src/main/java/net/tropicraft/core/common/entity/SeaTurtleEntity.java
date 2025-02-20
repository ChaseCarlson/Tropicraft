package net.tropicraft.core.common.entity;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.tropicraft.core.common.dimension.TropicraftDimension;
import net.tropicraft.core.common.entity.egg.SeaTurtleEggEntity;
import net.tropicraft.core.common.item.TropicraftItems;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

public class SeaTurtleEntity extends Turtle {

    private static final EntityDataAccessor<Boolean> IS_MATURE = SynchedEntityData.defineId(SeaTurtleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TURTLE_TYPE = SynchedEntityData.defineId(SeaTurtleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NO_BRAKES = SynchedEntityData.defineId(SeaTurtleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CAN_FLY = SynchedEntityData.defineId(SeaTurtleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_DIGGING = SynchedEntityData.defineId(SeaTurtleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_EGG = SynchedEntityData.defineId(SeaTurtleEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int NUM_TYPES = 6;
    
    private double lastPosY;
    private int digCounter;

    public SeaTurtleEntity(EntityType<? extends Turtle> type, Level world) {
        super(type, world);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public MobType getMobType() {
        return MobType.WATER;
    }

    @Override
    protected float nextStep() {
        return this.moveDist + 0.15F;
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType spawnReason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        setRandomTurtleType();
        this.lastPosY = getY();
        return super.finalizeSpawn(world, difficultyInstance, spawnReason, data, nbt);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // goals
        Set<WrappedGoal> goalSet = this.goalSelector.getAvailableGoals();

        final Optional<WrappedGoal> eggGoal = goalSet.stream().filter(p -> p.getGoal().toString().contains("Egg")).findFirst();
        if (eggGoal.isPresent()) {
            this.goalSelector.removeGoal(eggGoal.get().getGoal());
            this.goalSelector.addGoal(1, new BetterLayEggGoal(this, 1.0));
        }

        final Optional<WrappedGoal> mateGoal = goalSet.stream().filter(p -> p.getGoal().toString().contains("Mate")).findFirst();
        if (mateGoal.isPresent()) {
            this.goalSelector.removeGoal(mateGoal.get().getGoal());
            this.goalSelector.addGoal(1, new BetterMateGoal(this, 1.0));
        }
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(IS_MATURE, true);
        getEntityData().define(TURTLE_TYPE, 1);
        getEntityData().define(NO_BRAKES, false);
        getEntityData().define(CAN_FLY, false);
        getEntityData().define(IS_DIGGING, false);
        getEntityData().define(HAS_EGG, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("TurtleType", getTurtleType());
        nbt.putBoolean("IsMature", isMature());
        nbt.putBoolean("NoBrakesOnThisTrain", getNoBrakes());
        nbt.putBoolean("LongsForTheSky", getCanFly());
        nbt.putBoolean("HasEgg", hasEgg());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("TurtleType")) {
            setTurtleType(nbt.getInt("TurtleType"));
        } else {
            setRandomTurtleType();
        }
        if (nbt.contains("IsMature")) {
            setIsMature(nbt.getBoolean("IsMature"));
        } else {
            setIsMature(true);
        }
        setNoBrakes(nbt.getBoolean("NoBrakesOnThisTrain"));
        setCanFly(nbt.getBoolean("LongsForTheSky"));
        setHasEgg(nbt.getBoolean("HasEgg"));
        this.lastPosY = this.getY();
    }

    public boolean isMature() {
        return getEntityData().get(IS_MATURE);
    }

    public SeaTurtleEntity setIsMature(final boolean mature) {
        getEntityData().set(IS_MATURE, mature);
        return this;
    }

    public int getTurtleType() {
        return getEntityData().get(TURTLE_TYPE);
    }
    
    public void setRandomTurtleType() {
        setTurtleType(random.nextInt(NUM_TYPES) + 1);
    }

    public SeaTurtleEntity setTurtleType(final int type) {
        getEntityData().set(TURTLE_TYPE, Mth.clamp(type, 1, NUM_TYPES));
        return this;
    }
    
    public boolean getNoBrakes() {
        return getEntityData().get(NO_BRAKES);
    }
    
    public SeaTurtleEntity setNoBrakes(final boolean noBrakes) {
        getEntityData().set(NO_BRAKES, noBrakes);
        return this;
    }

    public boolean getCanFly() {
        return getEntityData().get(CAN_FLY);
    }
    
    public SeaTurtleEntity setCanFly(final boolean canFly) {
        getEntityData().set(CAN_FLY, canFly);
        return this;
    }
    
    @Override
    @Nullable
    public Entity getControllingPassenger() {
        final List<Entity> passengers = getPassengers();
        return passengers.isEmpty() ? null : passengers.get(0);
    }

    public static boolean canSpawnOnLand(EntityType<SeaTurtleEntity> turtle, LevelAccessor world, MobSpawnType reason, BlockPos pos, Random rand) {
        return pos.getY() < TropicraftDimension.getSeaLevel(world) + 4 && world.getBlockState(pos.below()).getBlock() == Blocks.SAND && world.getRawBrightness(pos, 0) > 8;
    }

    @Override
    public boolean canBeControlledByRider() {
        return getControllingPassenger() instanceof LivingEntity;
    }
    
    @Override
    public double getPassengersRidingOffset() {
        return super.getPassengersRidingOffset() - 0.1;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob partner) {
        return TropicraftEntities.SEA_TURTLE.get().create(this.level)
                .setTurtleType(random.nextBoolean() && partner instanceof SeaTurtleEntity ? ((SeaTurtleEntity)partner).getTurtleType() : getTurtleType())
                .setIsMature(false);
    }


    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = super.mobInteract(player, hand);
        if (result != InteractionResult.PASS) {
            return result;
        }

        if (!level.isClientSide && !player.isShiftKeyDown() && canAddPassenger(player) && isMature()) {
            player.startRiding(this);
        }

        return InteractionResult.SUCCESS;
    }
    
    @Override
    public boolean shouldRender(double x, double y, double z) {
        Entity controller = getControllingPassenger();
        if (controller != null) {
            return controller.shouldRender(x, y, z);
        }
        return super.shouldRender(x, y, z);
    }
    
    @Override
    public void tick() {
        super.tick();
        lastPosY = getY();
    }
    
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.isAlive() && this.isLayingEgg() && this.digCounter >= 1 && this.digCounter % 5 == 0) {
            BlockPos pos = this.blockPosition();
            if (this.level.getBlockState(pos.below()).getMaterial() == Material.SAND) {
                this.level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(Blocks.SAND.defaultBlockState()));
            }
        }

        if (this.level.isClientSide) {
            if (isVehicle() && canBeControlledByRider()) {
                if (isInWater() || getCanFly()) {
                    Vec3 movement = new Vec3(getX(), getY(), getZ()).subtract(xo, yo, zo);
                    double speed = movement.length();
                    Vec3 particleOffset = movement.reverse().scale(2);
                    if (speed > 0.05) {
                        int maxParticles = Mth.ceil(speed * 5);
                        int particlesToSpawn = random.nextInt(1 + maxParticles);
                        ParticleOptions particle = isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.END_ROD;
                        for (int i = 0; i < particlesToSpawn; i++) {
                            Vec3 particleMotion = movement.scale(1);
                            level.addParticle(particle, true,
                                    particleOffset.x() + getX() - 0.25 + random.nextDouble() * 0.5,
                                    particleOffset.y() + getY() + 0.1 + random.nextDouble() * 0.1,
                                    particleOffset.z() + getZ() - 0.25 + random.nextDouble() * 0.5, particleMotion.x, particleMotion.y, particleMotion.z);
                        }
                    }
                }
            }
        }
    }

    public float lerp(float x1, float x2, float t) {
        return x1 + (t*0.03f) * Mth.wrapDegrees(x2 - x1);
    }

    private float swimSpeedCurrent;

    @Override
    public void positionRider(Entity passenger) {
        super.positionRider(passenger);
        if (this.hasPassenger(passenger)) {
            if(passenger instanceof Player) {
                Player p = (Player)passenger;
                if(this.isInWater()) {
                    if(p.zza > 0f) {
                        this.setXRot(this.lerp(getXRot(), -(passenger.getXRot()*0.5f), 6f));
                        this.setYRot(this.lerp(getYRot(), -passenger.getYRot(), 6f));
//                        this.targetVector = null;
//                        this.targetVectorHeading = null;
                        this.swimSpeedCurrent += 0.05f;
                        if(this.swimSpeedCurrent > 4f) {
                            this.swimSpeedCurrent = 4f;
                        }
                    }
                    if(p.zza < 0f) {
                        this.swimSpeedCurrent *= 0.89f;
                        if(this.swimSpeedCurrent < 0.1f) {
                            this.swimSpeedCurrent = 0.1f;
                        }
                    }
                    if(p.zza == 0f) {
                        if(this.swimSpeedCurrent > 1f) {
                            this.swimSpeedCurrent *= 0.94f;
                            if(this.swimSpeedCurrent <= 1f) {
                                this.swimSpeedCurrent = 1f;
                            }
                        }
                        if(this.swimSpeedCurrent < 1f) {
                            this.swimSpeedCurrent *= 1.06f;
                            if(this.swimSpeedCurrent >= 1f) {
                                this.swimSpeedCurrent = 1f;
                            }
                        }
                        //this.swimSpeedCurrent = 1f;
                    }
                    //    this.swimYaw = -passenger.rotationYaw;
                }
                //p.rotationYaw = this.rotationYaw;
            } else
            if (passenger instanceof Mob) {
                Mob mobentity = (Mob)passenger;
                this.yBodyRot = mobentity.yBodyRot;
                this.yHeadRotO = mobentity.yHeadRotO;
            }
        }
    }
        
    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
    }

    @Override
    public void travel(Vec3 input) {
        if (isAlive()) {
            if (isVehicle() && canBeControlledByRider()) {
                final Entity controllingPassenger = getControllingPassenger();

                if (!(controllingPassenger instanceof LivingEntity)) {
                    return;
                }

                final LivingEntity controllingEntity = (LivingEntity) controllingPassenger;

                this.setYRot(controllingPassenger.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(controllingPassenger.getXRot());
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
                this.maxUpStep = 1.0F;
                this.flyingSpeed = this.getSpeed() * 0.1F;

                float strafe = controllingEntity.xxa;
                float forward = getNoBrakes() ? 1 : controllingEntity.zza;
                float vertical = controllingEntity.yya; // Players never use this?

                double verticalFromPitch = -Math.sin(Math.toRadians(getXRot())) * forward;
                forward *= Mth.clamp(1 - (Math.abs(getXRot()) / 90), 0.01f, 1);

                if (!isInWater()) {
                    if (getCanFly()) {
                        this.setDeltaMovement(this.getDeltaMovement().add(0, -this.getAttribute(ForgeMod.ENTITY_GRAVITY.get()).getValue() * 0.05, 0));
                    } else {
                        // Lower max speed when breaching, as a penalty to uncareful driving
                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.9, 0.99, 0.9).add(0, -this.getAttribute(ForgeMod.ENTITY_GRAVITY.get()).getValue(), 0));
                    }
                }

                if (this.isControlledByLocalInstance()) {
                    Vec3 travel = new Vec3(strafe, verticalFromPitch + vertical, forward)
                            .scale(this.getAttribute(Attributes.MOVEMENT_SPEED).getValue())
                            // This scale controls max speed. We reduce it significantly here so that the range of speed is higher
                            // This is compensated for by the high value passed to moveRelative
                            .scale(0.025F);
                    // This is the effective speed modifier, controls the post-scaling of the movement vector
                    moveRelative(1F, travel);
                    move(MoverType.SELF, getDeltaMovement());
                    // This value controls how much speed is "dampened" which effectively controls how much drift there is, and the max speed
                    this.setDeltaMovement(this.getDeltaMovement().scale(forward > 0 || !isInWater() ? 0.975 : 0.9));
                } else {
                    this.fallDistance = (float) Math.max(0, (getY() - lastPosY) * -8);
                    this.setDeltaMovement(Vec3.ZERO);
                }
                this.animationSpeedOld = this.animationSpeed;
                double d1 = this.getX() - this.xo;
                double d0 = this.getZ() - this.zo;
                float swinger = Mth.sqrt((float) (d1 * d1 + d0 * d0)) * 4.0F;
                if (swinger > 1.0F) {
                    swinger = 1.0F;
                }

                this.animationSpeed += (swinger - this.animationSpeed) * 0.4F;
                this.animationPosition += this.animationSpeed;
            } else {
                this.flyingSpeed = 0.02F;
                super.travel(input);
            }
        }
    }

    @Override
    public ItemStack getPickedResult(HitResult target) {
        return new ItemStack(TropicraftItems.SEA_TURTLE_SPAWN_EGG.get());
    }

    @Override
    public boolean canBeRiddenInWater(Entity rider) {
        return true;
    }

    static class BetterLayEggGoal extends MoveToBlockGoal {
        private final SeaTurtleEntity turtle;

        BetterLayEggGoal(SeaTurtleEntity turtle, double speedIn) {
            super(turtle, speedIn, 16);
            this.turtle = turtle;
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        @Override
        public boolean canUse() {
            return turtle.hasEgg() && turtle.getHomePos().closerToCenterThan(turtle.position(), 9.0D) && super.canUse();
        }

        /**
         * Returns whether an in-progress EntityAIBase should continue executing
         */
        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && turtle.hasEgg() && turtle.getHomePos().closerToCenterThan(turtle.position(), 9.0D);
        }

        /**
         * Keep ticking a continuous task that has already been started
         */
        @Override
        public void tick() {
            super.tick();
            BlockPos blockpos = this.turtle.blockPosition();
            if (!this.turtle.isInWater() && this.isReachedTarget()) {
                if (!this.turtle.isLayingEgg()) {
                    this.turtle.setDigging(true);
                } else if (this.turtle.digCounter > 200) {
                    Level world = this.turtle.level;
                    world.playSound(null, blockpos, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS, 0.3F, 0.9F + world.random.nextFloat() * 0.2F);
                    //world.setBlockState(this.destinationBlock.up(), Blocks.TURTLE_EGG.defaultBlockState().with(TurtleEggBlock.EGGS, Integer.valueOf(this.turtle.rand.nextInt(4) + 1)), 3);
                    final SeaTurtleEggEntity egg = TropicraftEntities.SEA_TURTLE_EGG.get().create(world);
                    final BlockPos spawnPos = blockPos.above();
                    egg.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
                    world.addFreshEntity(egg);
                    this.turtle.setHasEgg(false);
                    this.turtle.setDigging(false);
                    this.turtle.setInLoveTime(600);
                }

                if (this.turtle.isLayingEgg()) {
                    this.turtle.digCounter++;
                }
            }
        }

        @Override
        protected boolean isValidTarget(LevelReader worldIn, BlockPos pos) {
            if (!worldIn.isEmptyBlock(pos.above())) {
                return false;
            } else {
                return worldIn.getBlockState(pos).getMaterial() == Material.SAND;
            }
        }
    }

    static class BetterMateGoal extends BreedGoal {
        private final SeaTurtleEntity turtle;

        BetterMateGoal(SeaTurtleEntity turtle, double speedIn) {
            super(turtle, speedIn);
            this.turtle = turtle;
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        @Override
        public boolean canUse() {
            return super.canUse() && !this.turtle.hasEgg();
        }

        /**
         * Spawns a baby animal of the same type.
         */
        @Override
        protected void breed() {
            ServerPlayer serverplayerentity = this.animal.getLoveCause();
            if (serverplayerentity == null && this.partner.getLoveCause() != null) {
                serverplayerentity = this.partner.getLoveCause();
            }

            if (serverplayerentity != null) {
                serverplayerentity.awardStat(Stats.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(serverplayerentity, this.animal, this.partner, null);
            }

            this.turtle.setHasEgg(true);
            this.animal.resetLove();
            this.partner.resetLove();
            Random random = this.animal.getRandom();
            if (this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                this.level.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), random.nextInt(7) + 1));
            }

        }
    }

    private void setDigging(boolean digging) {
        this.digCounter = digging ? 1 : 0;
        this.entityData.set(IS_DIGGING, digging);
    }

    @Override
    public boolean isLayingEgg() {
        return digCounter > 0;
    }

    private void setHasEgg(boolean hasEgg) {
        this.entityData.set(HAS_EGG, hasEgg);
    }

    @Override
    public boolean hasEgg() {
        return entityData.get(HAS_EGG);
    }
}
