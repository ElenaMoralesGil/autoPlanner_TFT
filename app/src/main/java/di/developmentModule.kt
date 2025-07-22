package com.elena.autoplanner.di

import com.elena.autoplanner.FeatureFlags
import com.elena.autoplanner.domain.utils.DataSeeder
import com.elena.autoplanner.domain.utils.NoOpDataSeeder
import com.elena.autoplanner.domain.utils.RealDataSeeder
import org.koin.dsl.module

val developmentModule = module {

    single<DataSeeder> {
        if (FeatureFlags.ENABLE_TASK_SEEDING) {
            RealDataSeeder(
                taskRepository = get()
            )
        } else {
            NoOpDataSeeder()
        }
    }
}