package com.MySelfAlgoWithLoan.prod.database;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpreadDataRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL = """
        INSERT INTO spread_data (
            symbol, side, ts,
            bybit_futures_bid, bybit_futures_ask,
            bybit_futures_bids, bybit_futures_asks,
            kucoin_spot_bid, kucoin_spot_ask,
            kucoin_spot_bids, kucoin_spot_asks,
            net_spread_percent
        ) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?)
        """;

    public void save(SpreadDataRecord record) {
        jdbcTemplate.update(INSERT_SQL,
                record.symbol(),
                record.side(),
                Timestamp.from(record.timestamp()),
                record.bybitFuturesBid(),
                record.bybitFuturesAsk(),
                record.bybitFuturesBidsJson(),
                record.bybitFuturesAsksJson(),
                record.kucoinSpotBid(),
                record.kucoinSpotAsk(),
                record.kucoinSpotBidsJson(),
                record.kucoinSpotAsksJson(),
                record.netSpreadPercent()
        );
    }

    public void saveAll(List<SpreadDataRecord> records) {
        for (SpreadDataRecord record : records) {
            save(record);
        }
    }
}