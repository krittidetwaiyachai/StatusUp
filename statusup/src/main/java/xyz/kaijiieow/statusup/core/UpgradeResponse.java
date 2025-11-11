package xyz.kaijiieow.statusup.core;

public record UpgradeResponse(
    UpgradeResult result, 
    UpgradeDetails details
) {
    // ใช้สำหรับส่งผลลัพธ์ (เช่น NO_MONEY) และ details (เช่น ยศปัจจุบัน) กลับไปด้วยกัน
}