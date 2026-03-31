package com.example.springclaudeturtorial.phase3.autoconfig;

/**
 * Interface giả lập một "thư viện ngoài" mà chúng ta muốn
 * tự viết Auto-configuration cho nó.
 */
public interface SmsSender {
    void send(String phone, String message);
    String provider();
}
