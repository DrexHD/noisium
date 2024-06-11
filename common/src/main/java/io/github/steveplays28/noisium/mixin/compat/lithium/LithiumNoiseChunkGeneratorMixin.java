package io.github.steveplays28.noisium.mixin.compat.lithium;

import net.minecraft.block.BlockState;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NoiseChunkGenerator.class)
public abstract class LithiumNoiseChunkGeneratorMixin extends ChunkGenerator {
	@Shadow
	protected abstract Chunk populateNoise(Blender blender, StructureAccessor structureAccessor, NoiseConfig noiseConfig, Chunk chunk, int minimumCellY, int cellHeight);

	public LithiumNoiseChunkGeneratorMixin(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Redirect(method = "populateNoise(Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/chunk/Chunk;II)Lnet/minecraft/world/chunk/Chunk;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkSection;setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;"))
	private BlockState noisium$populateNoiseWrapSetBlockStateOperation(ChunkSection chunkSection, int chunkSectionBlockPosX, int chunkSectionBlockPosY, int chunkSectionBlockPosZ, BlockState blockState, boolean lock) {
		// Set the blockstate in the palette storage directly to improve performance
		var blockStateId = chunkSection.blockStateContainer.data.palette.index(blockState);
		chunkSection.blockStateContainer.data.storage().set(
				chunkSection.blockStateContainer.paletteProvider.computeIndex(chunkSectionBlockPosX, chunkSectionBlockPosY,
						chunkSectionBlockPosZ
				), blockStateId);

		return blockState;
	}

	/**
	 * @author Steveplays28, Drex
	 * @reason Replace enhanced for loops with fori loops, Fail-fast
	 */
	@Overwrite
	private Chunk method_38332(Chunk chunk, int generationShapeHeightFloorDiv, GenerationShapeConfig generationShapeConfig, int i, Blender blender, StructureAccessor structureAccessor, NoiseConfig noiseConfig, int minimumYFloorDiv) {
		// [VanillaCopy]
		int startingChunkSectionIndex = chunk.getSectionIndex(generationShapeHeightFloorDiv * generationShapeConfig.verticalCellBlockCount() - 1 + i);
		int minimumYChunkSectionIndex = chunk.getSectionIndex(i);
		// Vanilla
//		HashSet<ChunkSection> lockedSections = Sets.newHashSet();
//		for (int chunkSectionIndex = startingChunkSectionIndex; chunkSectionIndex >= minimumYChunkSectionIndex; --chunkSectionIndex) {
//			ChunkSection chunkSection = chunk.getSection(chunkSectionIndex);
//			chunkSection.lock();
//			lockedSections.add(chunkSection);
//		}
		// Noisium
		var chunkSections = chunk.getSectionArray();
		for (int chunkSectionIndex = startingChunkSectionIndex; chunkSectionIndex >= minimumYChunkSectionIndex; --chunkSectionIndex) {
			chunkSections[chunkSectionIndex].lock();
		}
		try {
			return this.populateNoise(blender, structureAccessor, noiseConfig, chunk, minimumYFloorDiv, generationShapeHeightFloorDiv);
		} finally {
			// Vanilla
//			for (ChunkSection lockedSection : lockedSections) {
//				lockedSection.unlock();
//			}
			// Noisium
			for (int chunkSectionIndex = startingChunkSectionIndex; chunkSectionIndex >= minimumYChunkSectionIndex; --chunkSectionIndex) {
				chunkSections[chunkSectionIndex].unlock();
			}
		}
	}
}
