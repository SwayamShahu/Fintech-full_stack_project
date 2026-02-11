package com.fintrack.config;

import com.fintrack.model.Category;
import com.fintrack.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            log.info("Initializing default categories...");
            
            List<Category> defaultCategories = Arrays.asList(
                Category.builder().name("Food & Dining").icon("🍔").color("#FF6B6B").isDefault(true).build(),
                Category.builder().name("Transportation").icon("🚗").color("#4ECDC4").isDefault(true).build(),
                Category.builder().name("Shopping").icon("🛍️").color("#45B7D1").isDefault(true).build(),
                Category.builder().name("Entertainment").icon("🎬").color("#96CEB4").isDefault(true).build(),
                Category.builder().name("Bills & Utilities").icon("💡").color("#FFEAA7").isDefault(true).build(),
                Category.builder().name("Healthcare").icon("🏥").color("#DDA0DD").isDefault(true).build(),
                Category.builder().name("Education").icon("📚").color("#98D8C8").isDefault(true).build(),
                Category.builder().name("Travel").icon("✈️").color("#F7DC6F").isDefault(true).build(),
                Category.builder().name("Groceries").icon("🛒").color("#82E0AA").isDefault(true).build(),
                Category.builder().name("Fitness").icon("💪").color("#85C1E9").isDefault(true).build(),
                Category.builder().name("Subscriptions").icon("📱").color("#BB8FCE").isDefault(true).build(),
                Category.builder().name("Other").icon("📦").color("#AEB6BF").isDefault(true).build()
            );

            categoryRepository.saveAll(defaultCategories);
            log.info("Initialized {} default categories", defaultCategories.size());
        }
    }
}
