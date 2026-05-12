package com.example.wardrobe.database

import android.content.Context
import androidx.room.Room
import com.example.wardrobe.database.daos.OutfitDao
import com.example.wardrobe.database.daos.OutfitItemDao
import com.example.wardrobe.database.daos.ScheduledItemDao
import com.example.wardrobe.database.daos.ScheduledOutfitDao
import com.example.wardrobe.database.daos.WardrobeItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wardrobe_db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideWardrobeItemDao(db: AppDatabase): WardrobeItemDao {
        return db.wardrobeItemDao()
    }

    @Provides
    @Singleton
    fun provideOutfitDao(db: AppDatabase): OutfitDao {
        return db.outfitDao()
    }

    @Provides
    @Singleton
    fun provideScheduledOutfitDao(db: AppDatabase): ScheduledOutfitDao {
        return db.scheduledOutfitDao()
    }

    @Provides
    @Singleton
    fun provideOutfitItemDao(db: AppDatabase): OutfitItemDao {
        return db.outfitItemDao()
    }

    @Provides
    @Singleton
    fun provideScheduledItemDao(db: AppDatabase): ScheduledItemDao {
        return db.scheduledItemDao()
    }

}