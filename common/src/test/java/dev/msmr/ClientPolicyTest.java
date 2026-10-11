package dev.msmr;
import dev.msmr.client.ClickPolicy;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class ClientPolicyTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    @Test void blocksIncompatiblePlacements() {
        Rule rule=new Rule(Rule.Mode.WHITELIST,List.of("minecraft:stone"),8);
        assertFalse(ClickPolicy.permits(rule,ItemStack.EMPTY,new ItemStack(Items.DIRT),1));
        assertTrue(ClickPolicy.permits(rule,ItemStack.EMPTY,new ItemStack(Items.STONE),1));
    }
    @Test void countsExistingStackAgainstCap() {
        Rule rule=new Rule(Rule.Mode.ALL,List.of(),8);
        assertTrue(ClickPolicy.permits(rule,new ItemStack(Items.STONE,7),new ItemStack(Items.STONE,64),1));
        assertFalse(ClickPolicy.permits(rule,new ItemStack(Items.STONE,8),new ItemStack(Items.STONE),1));
        assertFalse(ClickPolicy.permits(rule,ItemStack.EMPTY,new ItemStack(Items.STONE,64),64));
    }
    @Test void extractionDoesNotNeedPermissionToInsert() {
        assertTrue(ClickPolicy.permits(new Rule(Rule.Mode.BLOCK,List.of(),0),new ItemStack(Items.STONE),ItemStack.EMPTY,0));
    }
    @Test void replacementDoesNotCountDisplacedStack() {
        assertTrue(ClickPolicy.permits(new Rule(Rule.Mode.ALL,List.of(),8),new ItemStack(Items.DIRT,64),new ItemStack(Items.STONE,8),8));
    }
}
