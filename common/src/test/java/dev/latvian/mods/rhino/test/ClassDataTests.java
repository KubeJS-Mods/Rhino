package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.classdata.ClassDataCache;
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
		Context cx = Context.enter();
		var cache = new ClassDataCache(cx);
		var data = cache.of(Player.class);
		var member = data.getMember("x");
		System.out.println(member);
		Context.exit();
	}
}
