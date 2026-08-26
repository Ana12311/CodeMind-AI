package com.example.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

/**
 * 订单服务（示例代码，用于 CODE_REVIEW RAG 检索验证）
 */
public class OrderService {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/demo";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root123456";

    public void createOrder(String userId, String productId) {
        // SQL 拼接：存在注入风险
        String sql = "INSERT INTO orders(user_id, product_id) VALUES ('"
                + userId + "', '" + productId + "')";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            // 吞掉异常，未记录日志
        }
    }

    public double calcTotal(List<Double> prices) {
        double total = 0;
        for (double p : prices) {
            total += p;
        }
        return total;
    }
}
