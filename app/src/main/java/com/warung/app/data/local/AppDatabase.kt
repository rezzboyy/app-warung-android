package com.warung.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.warung.app.data.local.dao.CashFlowDao
import com.warung.app.data.local.dao.CategoryDao
import com.warung.app.data.local.dao.DebtDao
import com.warung.app.data.local.dao.ProductDao
import com.warung.app.data.local.dao.TransactionDao
import com.warung.app.data.local.entity.CashFlowEntity
import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.data.local.entity.DebtEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.data.local.entity.TransactionItemEntity
import com.warung.app.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        CustomerEntity::class,
        DebtEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        CashFlowEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun debtDao(): DebtDao
    abstract fun cashFlowDao(): CashFlowDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialCategories(database.categoryDao())
                    }
                }
            }

            private suspend fun populateInitialCategories(categoryDao: CategoryDao) {
                val defaultCategories = listOf(
                    CategoryEntity(name = "Sembako"),
                    CategoryEntity(name = "Makanan & Minuman"),
                    CategoryEntity(name = "Snack"),
                    CategoryEntity(name = "Rokok"),
                    CategoryEntity(name = "Kebutuhan Rumah"),
                    CategoryEntity(name = "Lain-lain")
                )
                categoryDao.insertAll(defaultCategories)
            }
        }
    }
}
