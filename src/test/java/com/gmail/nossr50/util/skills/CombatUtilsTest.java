package com.gmail.nossr50.util.skills;

import static java.util.logging.Logger.getLogger;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.meta.HealthbarSnapshot;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.MetadataConstants;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CombatUtilsTest extends MMOTestEnvironment {
    private static final java.util.logging.Logger logger = getLogger(
            CombatUtilsTest.class.getName());

    private LivingEntity targetEntity;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        targetEntity = Mockito.mock(LivingEntity.class);
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    @Nested
    class FixNames {

        @Test
        void restoresNameAndVisibilityFromSnapshot() {
            final HealthbarSnapshot snapshot = new HealthbarSnapshot("Boss Slime", true, 0L);
            final FixedMetadataValue metaValue = new FixedMetadataValue(mcMMO.p, snapshot);

            when(targetEntity.hasMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(true);
            when(targetEntity.getMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(List.of(metaValue));

            CombatUtils.fixNames(targetEntity);

            verify(targetEntity).setCustomName("Boss Slime");
            verify(targetEntity).setCustomNameVisible(true);
            verify(targetEntity).removeMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT,
                    mcMMO.p);
        }

        @Test
        void restoresNullNameWhenSnapshotHadNoCustomName() {
            final HealthbarSnapshot snapshot = new HealthbarSnapshot(null, false, 0L);
            final FixedMetadataValue metaValue = new FixedMetadataValue(mcMMO.p, snapshot);

            when(targetEntity.hasMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(true);
            when(targetEntity.getMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(List.of(metaValue));

            CombatUtils.fixNames(targetEntity);

            verify(targetEntity).setCustomName((String) null);
            verify(targetEntity).setCustomNameVisible(false);
            verify(targetEntity).removeMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT,
                    mcMMO.p);
        }

        @Test
        void doesNothingWhenNoSnapshotExists() {
            when(targetEntity.hasMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(false);
            when(targetEntity.getMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(Collections.emptyList());

            CombatUtils.fixNames(targetEntity);

            verify(targetEntity, never()).setCustomName(Mockito.any());
            verify(targetEntity, never()).setCustomNameVisible(Mockito.anyBoolean());
            verify(targetEntity, never()).removeMetadata(
                    MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT, mcMMO.p);
        }
    }

    @Nested
    class RestoreMobNameIfLethal {

        @Test
        void restoresWhenLethalAndSnapshotPresent() {
            // Given – damage will kill the entity, snapshot is present
            final HealthbarSnapshot snapshot = new HealthbarSnapshot("Named Mob", true, 0L);
            final FixedMetadataValue metaValue = new FixedMetadataValue(mcMMO.p, snapshot);

            final EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);
            when(event.getEntity()).thenReturn(targetEntity);
            when(event.getFinalDamage()).thenReturn(10.0);
            when(targetEntity.getHealth()).thenReturn(5.0); // damage > health → lethal

            when(targetEntity.hasMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(true);
            when(targetEntity.getMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(List.of(metaValue));

            // When
            CombatUtils.restoreMobNameIfLethal(event);

            // Then
            verify(targetEntity).setCustomName("Named Mob");
            verify(targetEntity).setCustomNameVisible(true);
            verify(targetEntity).removeMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT,
                    mcMMO.p);
        }

        @Test
        void skipsWhenNonLethal() {
            // Given – entity survives
            final EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);
            when(event.getEntity()).thenReturn(targetEntity);
            when(event.getFinalDamage()).thenReturn(3.0);
            when(targetEntity.getHealth()).thenReturn(10.0); // damage < health → survives

            // When
            CombatUtils.restoreMobNameIfLethal(event);

            // Then – snapshot never consulted
            verify(targetEntity, never()).hasMetadata(
                    MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT);
            verify(targetEntity, never()).setCustomName(Mockito.any());
            verify(targetEntity, never()).setCustomNameVisible(Mockito.anyBoolean());
        }

        @Test
        void skipsWhenNoSnapshot() {
            // Given – lethal hit but entity was never given a healthbar
            final EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);
            when(event.getEntity()).thenReturn(targetEntity);
            when(event.getFinalDamage()).thenReturn(20.0);
            when(targetEntity.getHealth()).thenReturn(5.0); // lethal

            when(targetEntity.hasMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(false);

            // When
            CombatUtils.restoreMobNameIfLethal(event);

            // Then
            verify(targetEntity, never()).setCustomName(Mockito.any());
            verify(targetEntity, never()).setCustomNameVisible(Mockito.anyBoolean());
            verify(targetEntity, never()).removeMetadata(
                    MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT, mcMMO.p);
        }

        @Test
        void skipsNonLivingEntity() {
            // Given – event entity is not a LivingEntity
            final EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);
            final org.bukkit.entity.Entity nonLivingEntity = Mockito.mock(
                    org.bukkit.entity.Entity.class);
            when(event.getEntity()).thenReturn(nonLivingEntity);

            // When
            CombatUtils.restoreMobNameIfLethal(event);

            // Then – no living-entity interaction at all
            verify(targetEntity, never()).setCustomName(Mockito.any());
            verify(targetEntity, never()).setCustomNameVisible(Mockito.anyBoolean());
        }

        @Test
        void restoresNullCustomNameCorrectly() {
            // Given – entity had no custom name before healthbar was applied
            final HealthbarSnapshot snapshot = new HealthbarSnapshot(null, false, 0L);
            final FixedMetadataValue metaValue = new FixedMetadataValue(mcMMO.p, snapshot);

            final EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);
            when(event.getEntity()).thenReturn(targetEntity);
            when(event.getFinalDamage()).thenReturn(50.0);
            when(targetEntity.getHealth()).thenReturn(1.0); // lethal

            when(targetEntity.hasMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(true);
            when(targetEntity.getMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(List.of(metaValue));

            // When
            CombatUtils.restoreMobNameIfLethal(event);

            // Then – null name is restored, not ""
            verify(targetEntity).setCustomName((String) null);
            verify(targetEntity).setCustomNameVisible(false);
            verify(targetEntity).removeMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT,
                    mcMMO.p);
        }

        @Test
        void exactLethalBoundaryTriggersRestore() {
            // Given – final damage exactly equals health (boundary: exactly lethal)
            final HealthbarSnapshot snapshot = new HealthbarSnapshot("Boundary Mob", false, 0L);
            final FixedMetadataValue metaValue = new FixedMetadataValue(mcMMO.p, snapshot);

            final EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);
            when(event.getEntity()).thenReturn(targetEntity);
            when(event.getFinalDamage()).thenReturn(10.0);
            when(targetEntity.getHealth()).thenReturn(10.0); // exactly equal → lethal

            when(targetEntity.hasMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(true);
            when(targetEntity.getMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT))
                    .thenReturn(List.of(metaValue));

            // When
            CombatUtils.restoreMobNameIfLethal(event);

            // Then – should restore (damage >= health)
            verify(targetEntity).setCustomName("Boundary Mob");
            verify(targetEntity).removeMetadata(MetadataConstants.METADATA_KEY_HEALTHBAR_SNAPSHOT,
                    mcMMO.p);
        }
    }
}

