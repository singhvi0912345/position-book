package com.bank.positionbook.config;

import com.bank.positionbook.engine.PositionBook;
import com.bank.positionbook.store.InMemoryPositionBookStore;
import com.bank.positionbook.store.PositionBookStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PositionBookConfig {

    @Bean
    public PositionBookStore positionBookStore() {
        return new InMemoryPositionBookStore();
    }

    @Bean
    public PositionBook positionBook(PositionBookStore positionBookStore) {
        return new PositionBook(positionBookStore);
    }
}
