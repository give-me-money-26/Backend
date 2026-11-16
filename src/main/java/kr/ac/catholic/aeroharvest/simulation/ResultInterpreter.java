package kr.ac.catholic.aeroharvest.simulation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * 시뮬레이션 결과로 화면 "결과 해석" 문장을 만듭니다.
 * 판단 순서: ① 계산 불가·0 걸러내기 → ② 회수기간 구간 → ③ 주된 이유 하나 → ④ 숫자 표시 규칙.
 * 기준값은 모두 아래 상수에 모아 두었습니다. 기획 확정에 따라 여기만 고치면 됩니다.
 */
final class ResultInterpreter {
    /** 이보다 낮으면 "풍속이 낮다"고 설명 (m/s). 제안값 — 소형 터빈 시동 풍속 근처. */
    static final double LOW_WIND_MS = 3.0;
    /** 이보다 낮으면 "바람 에너지의 절반 이상을 놓친다"고 설명. */
    static final double POOR_PLACEMENT_FACTOR = 0.5;
    /** 이 이상이면 "배기구 가까이 배치되어 바람을 잘 받는다"고 설명. */
    static final double GOOD_PLACEMENT_FACTOR = 0.8;
    /** 이보다 짧으면 "운전시간이 짧다"고 설명 (시간/일). */
    static final double SHORT_OPERATING_HOURS = 8.0;
    /** 회수기간 구간 경계 (년). 20년 = 일반적인 소형 풍력 터빈 수명 가정. 100년 = 프론트 PAYBACK_LIMIT_YEARS. */
    static final double PAYBACK_GOOD_YEARS = 10.0;
    static final double TURBINE_LIFETIME_YEARS = 20.0;
    static final double PAYBACK_LIMIT_YEARS = 100.0;
    /** 30년생 소나무 1그루 연간 CO2 흡수량 (kg). 국립산림과학원 자료로 흔히 인용되는 값 — 원문 확인 필요. */
    static final double PINE_KG_CO2_PER_YEAR = 6.6;
    /** 이 이상이면 "캠퍼스 활동으로 내세울 만한 수준" (그루). */
    static final double MANY_TREES = 10.0;

    private ResultInterpreter() {}

    /** 해석에 필요한 값 묶음. */
    record Facts(String buildingName, SourceMode sourceMode, Integer hour, double windSpeedMS,
                 double efficiencyPercent, double collectionAreaM2, double distanceM, double angleDegrees,
                 double operatingHoursPerDay, double placementFactor, double recoveredPowerW,
                 double annualEnergyKwh, double annualSavingsKrw, Double paybackYears,
                 double annualCarbonKg, double carbonFactorKgPerKwh) {}

    static String environment(Facts f) {
        String subject = withTopicParticle(f.buildingName());

        // ① 계산 불가·0
        if (f.annualEnergyKwh() <= 0) return subject + " " + noEnergyReason(f) + " 탄소 저감 효과도 없습니다.";
        String energy = "연간 약 " + kwh(f.annualEnergyKwh()) + " kWh를";
        if (f.carbonFactorKgPerKwh() <= 0 || f.annualCarbonKg() <= 0) {
            return subject + " " + energy + " 회수하지만, 배출계수가 0으로 입력되어 탄소 저감량을 계산할 수 없습니다.";
        }

        // ② 소나무 환산 구간별 비유 + 결론
        String head = subject + " " + energy + " 회수해 탄소 약 " + kg(f.annualCarbonKg()) + " kg을 줄일 수 있습니다. ";
        double trees = f.annualCarbonKg() / PINE_KG_CO2_PER_YEAR;
        if (trees < 1) {
            long percent = Math.round(trees * 100);
            String share = percent < 1 ? "1% 미만" : "약 " + percent + "%";
            return head + "30년생 소나무 한 그루가 1년 동안 흡수하는 양의 " + share + "입니다."
                    + " 한 대만으로는 효과가 작아, 여러 배기구에 함께 설치할 때 의미가 커집니다.";
        }
        if (trees < MANY_TREES) {
            return head + "30년생 소나무 약 " + format(trees, "#,##0.#") + "그루가 1년 동안 흡수하는 양입니다."
                    + " 건물 한 곳의 탄소 저감 실천 사례로 보여 주기 좋은 수준입니다.";
        }
        return head + "30년생 소나무 약 " + format(Math.round(trees), "#,##0") + "그루가 1년 동안 흡수하는 양입니다."
                + " 캠퍼스 탄소 저감 활동으로 내세울 만한 수준입니다.";
    }

    static String kg(double value) {
        return value < 1 ? format(value, "#,##0.00") : format(value, "#,##0.0");
    }

    static String revenue(Facts f) {
        String subject = withTopicParticle(f.buildingName());

        // ① 계산 불가·0
        if (f.annualEnergyKwh() <= 0) return subject + " " + noEnergyReason(f);
        String energy = "연간 약 " + kwh(f.annualEnergyKwh()) + " kWh를";
        if (f.annualSavingsKrw() <= 0) {
            return subject + " " + energy + " 회수하지만, 전기요금이 0원으로 입력되어 절감액과 회수기간을 계산할 수 없습니다.";
        }
        String saving = energy + " 회수해 연 " + krw(f.annualSavingsKrw()) + "을 아낄 수 있";
        if (f.paybackYears() == null) return subject + " " + saving + "습니다.";

        // ② 회수기간 구간 + ③ 이유 + 결론
        double payback = f.paybackYears();
        StringBuilder text = new StringBuilder()
                .append(subject).append(' ').append(saving).append("어, 설치비 회수에 ")
                .append(years(payback)).append(" 걸립니다. ")
                .append(payback > TURBINE_LIFETIME_YEARS ? slowReason(f) : fastReason(f));
        if (payback <= PAYBACK_GOOD_YEARS) {
            text.append(" 터빈 수명 안에 충분히 회수할 수 있는 조건입니다.");
        } else if (payback > PAYBACK_LIMIT_YEARS) {
            text.append(" 사실상 설치비 회수가 어려워, 경제성보다 실증·교육 목적에 가깝습니다.");
        }
        return text.toString();
    }

    private static String noEnergyReason(Facts f) {
        if (f.windSpeedMS() <= 0) {
            return f.sourceMode() == SourceMode.TIME_SLOT && f.hour() != null
                    ? f.hour() + "시에 배기 바람이 없어 회수할 전력이 없습니다."
                    : "입력한 풍속이 0 m/s라 회수할 전력이 없습니다.";
        }
        if (f.efficiencyPercent() <= 0) return "변환 효율이 0%로 입력되어 회수할 전력이 없습니다.";
        if (f.operatingHoursPerDay() <= 0) return "하루 운전시간이 0시간으로 입력되어 연간 발전량이 없습니다.";
        return "현재 조건에서는 회수할 전력이 없습니다.";
    }

    /** 회수기간이 터빈 수명보다 길 때: 처음 해당하는 이유 하나. */
    private static String slowReason(Facts f) {
        if (f.placementFactor() < POOR_PLACEMENT_FACTOR) {
            long angle = Math.round(Math.abs(f.angleDegrees()));
            String where;
            if (angle == 0) where = "배기구에서 " + one(f.distanceM()) + " m 떨어져 있어";
            else if (f.distanceM() <= 1.0) where = "터빈 각도가 " + angle + "°라";
            else where = "배기구에서 " + one(f.distanceM()) + " m 떨어져 있고 각도가 " + angle + "°라";
            return where + " 바람 에너지의 절반 이상을 놓치기 때문입니다.";
        }
        if (f.windSpeedMS() < LOW_WIND_MS) {
            return "배기 풍속이 약 " + one(f.windSpeedMS()) + " m/s로 낮아 회수 전력이 " + watt(f.recoveredPowerW())
                    + " W 수준이기 때문입니다.";
        }
        if (f.operatingHoursPerDay() < SHORT_OPERATING_HOURS) {
            return "하루 운전시간이 " + plain(f.operatingHoursPerDay()) + "시간으로 짧아 연간 발전량이 적기 때문입니다.";
        }
        return "풍속은 " + one(f.windSpeedMS()) + " m/s로 양호하지만 회수 면적이 " + plain(f.collectionAreaM2())
                + "㎡로 작아 회수 전력이 " + watt(f.recoveredPowerW()) + " W에 그치기 때문입니다.";
    }

    /** 회수기간이 터빈 수명 안일 때. */
    private static String fastReason(Facts f) {
        if (f.windSpeedMS() >= LOW_WIND_MS && f.placementFactor() >= GOOD_PLACEMENT_FACTOR) {
            return "배기 풍속이 " + one(f.windSpeedMS()) + " m/s로 높고 배기구 가까이 배치되어 바람을 잘 받기 때문입니다.";
        }
        return "현재 입력한 조건에서 회수 전력이 " + watt(f.recoveredPowerW()) + " W로 확보되기 때문입니다.";
    }

    // ④ 숫자 표시 규칙

    static String kwh(double value) {
        return value < 1 ? format(value, "#,##0.00") : format(value, "#,##0.0");
    }

    static String krw(double value) {
        return value < 1 ? "1원 미만" : format(Math.round(value), "#,##0") + "원";
    }

    static String years(double value) {
        if (value > PAYBACK_LIMIT_YEARS) return "100년 이상이";
        if (value < 1) return "1년 미만이";
        return "약 " + Math.round(value) + "년이";
    }

    private static String watt(double value) {
        return value < 10 ? format(value, "#,##0.0") : format(value, "#,##0");
    }

    private static String one(double value) {
        return format(value, "0.0");
    }

    private static String plain(double value) {
        return format(value, "#,##0.##");
    }

    private static String format(double value, String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.KOREA)).format(value);
    }

    /** 받침이 있으면 "은", 없으면 "는". */
    static String withTopicParticle(String name) {
        if (name == null || name.isEmpty()) return "이 건물은";
        char last = name.charAt(name.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) return name + "은";
        return name + ((last - 0xAC00) % 28 == 0 ? "는" : "은");
    }
}
