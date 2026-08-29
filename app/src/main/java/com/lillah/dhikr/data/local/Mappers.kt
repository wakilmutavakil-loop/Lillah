package com.lillah.dhikr.data.local

import com.lillah.dhikr.data.local.entity.CollectionEntity
import com.lillah.dhikr.data.local.entity.DhikrEntity
import com.lillah.dhikr.domain.model.CollectionKind
import com.lillah.dhikr.domain.model.CoverArt
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.domain.model.DhikrCollection

fun DhikrEntity.toDomain(): Dhikr = Dhikr(
    id = id,
    name = name,
    arabic = arabic,
    transliteration = transliteration,
    meaning = meaning,
    virtue = virtue,
    source = source,
    targetCount = targetCount,
    dailyTarget = dailyTarget,
    collectionId = collectionId,
    sortOrder = sortOrder,
    accentIndex = accentIndex,
    isFavorite = isFavorite,
    isArchived = isArchived,
    isBuiltIn = isBuiltIn,
    currentCount = currentCount,
    roundsToday = roundsToday,
    roundsEpochDay = roundsEpochDay,
    lastCountedAt = lastCountedAt,
    createdAt = createdAt,
)

fun Dhikr.toEntity(): DhikrEntity = DhikrEntity(
    id = id,
    name = name,
    arabic = arabic,
    transliteration = transliteration,
    meaning = meaning,
    virtue = virtue,
    source = source,
    targetCount = targetCount,
    dailyTarget = dailyTarget,
    collectionId = collectionId,
    sortOrder = sortOrder,
    accentIndex = accentIndex,
    isFavorite = isFavorite,
    isArchived = isArchived,
    isBuiltIn = isBuiltIn,
    currentCount = currentCount,
    roundsToday = roundsToday,
    roundsEpochDay = roundsEpochDay,
    lastCountedAt = lastCountedAt,
    createdAt = createdAt,
)

fun CollectionEntity.toDomain(): DhikrCollection = DhikrCollection(
    id = id,
    name = name,
    arabicName = arabicName,
    description = description,
    kind = CollectionKind.fromName(kind),
    artwork = CoverArt.fromKey(artworkKey),
    coverImagePath = coverImagePath,
    accentIndex = accentIndex,
    sortOrder = sortOrder,
    isBuiltIn = isBuiltIn,
)

fun DhikrCollection.toEntity(): CollectionEntity = CollectionEntity(
    id = id,
    name = name,
    arabicName = arabicName,
    description = description,
    kind = kind.name,
    artworkKey = artwork.name,
    coverImagePath = coverImagePath,
    accentIndex = accentIndex,
    sortOrder = sortOrder,
    isBuiltIn = isBuiltIn,
)
