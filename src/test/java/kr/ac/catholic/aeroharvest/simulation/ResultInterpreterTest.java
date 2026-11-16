package kr.ac.catholic.aeroharvest.simulation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultInterpreterTest {

    /** 니콜스관 기본 조건(풍속 3.14, 거리 2 m, 각도 0°, 8시간, 터빈 1)과 같은 값. */
    private static Builder nicholls() {
        return new Builder();
    }

    // ── 수익 분석 ──

    @Test
    void defaultNichollsExplainsSmallAreaWithoutConclusion() {
        String text = ResultInterpreter.revenue(nicholls().facts());

        assertThat(text).startsWith("니콜스관은 연간 약 9.7 kWh를 회수해 연 1,454원을 아낄 수 있어, 설치비 회수에 약 69년이 걸립니다.");
        assertThat(text).contains("풍속은 3.1 m/s로 양호하지만 회수 면적이 0.8㎡로 작아 회수 전력이 3.3 W에 그치기 때문입니다.");
        assertThat(text).doesNotContain("터빈 수명 안에", "사실상");
    }

    @Test
    void lowWindIsTheReasonWhenPlacementIsFine() {
        String text = ResultInterpreter.revenue(nicholls().name("학관").wind(2.44).power(1.56)
                .energy(4.56).savings(684.6).payback(146.1).facts());

        assertThat(text).contains("설치비 회수에 100년 이상이 걸립니다.");
        assertThat(text).contains("배기 풍속이 약 2.4 m/s로 낮아 회수 전력이 1.6 W 수준이기 때문입니다.");
        assertThat(text).endsWith("사실상 설치비 회수가 어려워, 경제성보다 실증·교육 목적에 가깝습니다.");
    }

    @Test
    void poorPlacementComesBeforeLowWind() {
        String text = ResultInterpreter.revenue(nicholls().wind(2.44).distance(8).angle(-60)
                .placement(0.1).payback(1500.0).facts());

        assertThat(text).contains("배기구에서 8.0 m 떨어져 있고 각도가 60°라 바람 에너지의 절반 이상을 놓치기 때문입니다.");
    }

    @Test
    void poorPlacementByDistanceOnly() {
        String text = ResultInterpreter.revenue(nicholls().distance(7).angle(0).placement(0.46).facts());

        assertThat(text).contains("배기구에서 7.0 m 떨어져 있어 바람 에너지의 절반 이상을 놓치기 때문입니다.");
    }

    @Test
    void poorPlacementByAngleOnly() {
        String text = ResultInterpreter.revenue(nicholls().distance(1).angle(50).placement(0.41).facts());

        assertThat(text).contains("터빈 각도가 50°라 바람 에너지의 절반 이상을 놓치기 때문입니다.");
    }

    @Test
    void shortOperatingHours() {
        String text = ResultInterpreter.revenue(nicholls().hours(4).facts());

        assertThat(text).contains("하루 운전시간이 4시간으로 짧아 연간 발전량이 적기 때문입니다.");
    }

    @Test
    void fastPaybackWithGoodWindAndPlacement() {
        String text = ResultInterpreter.revenue(nicholls().wind(6).placement(0.9).payback(5.2).facts());

        assertThat(text).contains("설치비 회수에 약 5년이 걸립니다.");
        assertThat(text).contains("배기 풍속이 6.0 m/s로 높고 배기구 가까이 배치되어 바람을 잘 받기 때문입니다.");
        assertThat(text).endsWith("터빈 수명 안에 충분히 회수할 수 있는 조건입니다.");
    }

    @Test
    void mediumPaybackWithoutConclusion() {
        String text = ResultInterpreter.revenue(nicholls().wind(2.5).payback(15.0).facts());

        assertThat(text).endsWith("현재 입력한 조건에서 회수 전력이 3.3 W로 확보되기 때문입니다.");
    }

    @Test
    void noWindAtTimeSlot() {
        String text = ResultInterpreter.revenue(nicholls().timeSlot(3).wind(0).power(0).energy(0)
                .savings(0).payback(null).facts());

        assertThat(text).isEqualTo("니콜스관은 3시에 배기 바람이 없어 회수할 전력이 없습니다.");
    }

    @Test
    void zeroEfficiency() {
        String text = ResultInterpreter.revenue(nicholls().efficiency(0).power(0).energy(0)
                .savings(0).payback(null).facts());

        assertThat(text).isEqualTo("니콜스관은 변환 효율이 0%로 입력되어 회수할 전력이 없습니다.");
    }

    @Test
    void zeroOperatingHours() {
        String text = ResultInterpreter.revenue(nicholls().hours(0).energy(0).savings(0).payback(null).facts());

        assertThat(text).isEqualTo("니콜스관은 하루 운전시간이 0시간으로 입력되어 연간 발전량이 없습니다.");
    }

    @Test
    void zeroTariff() {
        String text = ResultInterpreter.revenue(nicholls().savings(0).payback(null).facts());

        assertThat(text).isEqualTo("니콜스관은 연간 약 9.7 kWh를 회수하지만, 전기요금이 0원으로 입력되어 절감액과 회수기간을 계산할 수 없습니다.");
    }

    @Test
    void noTurbineOmitsPayback() {
        String text = ResultInterpreter.revenue(nicholls().payback(null).facts());

        assertThat(text).isEqualTo("니콜스관은 연간 약 9.7 kWh를 회수해 연 1,454원을 아낄 수 있습니다.");
    }

    @Test
    void tinyNumbersUseSpecialFormats() {
        String text = ResultInterpreter.revenue(nicholls().energy(0.004).savings(0.6).payback(160000.0).facts());

        assertThat(text).contains("연간 약 0.00 kWh를 회수해 연 1원 미만을 아낄 수 있어, 설치비 회수에 100년 이상이 걸립니다.");
    }

    @Test
    void topicParticleFollowsFinalConsonant() {
        assertThat(ResultInterpreter.withTopicParticle("다솔관")).isEqualTo("다솔관은");
        assertThat(ResultInterpreter.withTopicParticle("마리아")).isEqualTo("마리아는");
        assertThat(ResultInterpreter.withTopicParticle("")).isEqualTo("이 건물은");
    }

    // ── 환경 영향 ──

    @Test
    void lessThanOneTree() {
        String text = ResultInterpreter.environment(nicholls().facts());

        assertThat(text).isEqualTo("니콜스관은 연간 약 9.7 kWh를 회수해 탄소 약 3.9 kg을 줄일 수 있습니다. "
                + "30년생 소나무 한 그루가 1년 동안 흡수하는 양의 약 59%입니다. "
                + "한 대만으로는 효과가 작아, 여러 배기구에 함께 설치할 때 의미가 커집니다.");
    }

    @Test
    void severalTrees() {
        String text = ResultInterpreter.environment(nicholls().energy(50).carbon(20).facts());

        assertThat(text).contains("30년생 소나무 약 3그루가 1년 동안 흡수하는 양입니다.");
        assertThat(text).endsWith("건물 한 곳의 탄소 저감 실천 사례로 보여 주기 좋은 수준입니다.");
    }

    @Test
    void manyTrees() {
        String text = ResultInterpreter.environment(nicholls().energy(250).carbon(100).facts());

        assertThat(text).contains("30년생 소나무 약 15그루가 1년 동안 흡수하는 양입니다.");
        assertThat(text).endsWith("캠퍼스 탄소 저감 활동으로 내세울 만한 수준입니다.");
    }

    @Test
    void zeroCarbonFactor() {
        String text = ResultInterpreter.environment(nicholls().carbon(0).factor(0).facts());

        assertThat(text).isEqualTo("니콜스관은 연간 약 9.7 kWh를 회수하지만, 배출계수가 0으로 입력되어 탄소 저감량을 계산할 수 없습니다.");
    }

    @Test
    void noEnergyMeansNoCarbonReduction() {
        String text = ResultInterpreter.environment(nicholls().wind(0).power(0).energy(0).savings(0)
                .payback(null).carbon(0).facts());

        assertThat(text).isEqualTo("니콜스관은 입력한 풍속이 0 m/s라 회수할 전력이 없습니다. 탄소 저감 효과도 없습니다.");
    }

    private static final class Builder {
        String name = "니콜스관";
        SourceMode mode = SourceMode.DIRECT_SPEED;
        Integer hour = null;
        double wind = 3.14, efficiency = 25, area = 0.8, distance = 2, angle = 0, hours = 8;
        double placement = 0.878, power = 3.32, energy = 9.69, savings = 1453.8;
        Double payback = 68.8;
        double carbon = 3.88, factor = 0.4;

        Builder name(String v) { name = v; return this; }
        Builder timeSlot(int h) { mode = SourceMode.TIME_SLOT; hour = h; return this; }
        Builder wind(double v) { wind = v; return this; }
        Builder efficiency(double v) { efficiency = v; return this; }
        Builder distance(double v) { distance = v; return this; }
        Builder angle(double v) { angle = v; return this; }
        Builder hours(double v) { hours = v; return this; }
        Builder placement(double v) { placement = v; return this; }
        Builder power(double v) { power = v; return this; }
        Builder energy(double v) { energy = v; return this; }
        Builder savings(double v) { savings = v; return this; }
        Builder payback(Double v) { payback = v; return this; }
        Builder carbon(double v) { carbon = v; return this; }
        Builder factor(double v) { factor = v; return this; }

        ResultInterpreter.Facts facts() {
            return new ResultInterpreter.Facts(name, mode, hour, wind, efficiency, area, distance, angle, hours,
                    placement, power, energy, savings, payback, carbon, factor);
        }
    }
}
