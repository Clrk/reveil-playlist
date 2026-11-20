package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.CoverCrop
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestFactory

class PochetteWorld {
    var crop = CoverCrop.Square(0f, 0f, 0f)
}

class PochetteTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/pochette.feature", ::PochetteWorld) {
        step("la pochette fait \"(\\d+)\" sur \"(\\d+)\" pixels") { (w, h) -> crop = CoverCrop.of(w.toFloat(), h.toFloat()) }
        step("on garde le carré de \"(\\d+)\" pixels à partir de \"(\\d+)\", \"(\\d+)\"") { (side, x, y) ->
            assertEquals(CoverCrop.Square(x.toFloat(), y.toFloat(), side.toFloat()), crop)
        }
    }
}
