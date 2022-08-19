package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.SharedContextData;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClassDataTests {
	@Test
	@DisplayName("Class Data")
	public void classData() {
		Context cx = Context.enterWithNewFactory();
		var scope = cx.initStandardObjects();
		var contextData = SharedContextData.get(scope);
		var cache = contextData.getClassDataCache();
		var data = cache.of(Player.class);
		var member = data.getMember("x");
		System.out.println(member);
		Context.exit();
	}
}
