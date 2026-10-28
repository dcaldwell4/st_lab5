package ObjectOriented;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

public class SpellCastingTest {
    Wizard wizard = new Wizard(5, 100);
    // Challenge 1: The Infinite Exhaustion Trap (State Transition Leak)
    @Test
    public void rest_whenExhausted_resetsStanceToIdle() throws HeroLevelException, InvalidStateException, ManaInvestedException, WizardException {
        wizard.beginChanneling();

        int manaInvested = 1; // minimum mana invested for spellSurge
        // exhaust wizard
        for (int i = 0; i<4; i++){
            wizard.spellSurge(manaInvested);
        }

        // test for exhausted state then call rest()
        assertEquals(Wizard.Stance.EXHAUSTED, wizard.getCurrentStance());
        wizard.rest();

        assertEquals(600, wizard.getManaStore());
        assertEquals(Wizard.Stance.IDLE, wizard.getCurrentStance());
    }

    // Challenge 2: Unchannelled Surge Execution (Precondition Bypass)
    @Test
    public void stateVerificationTest() throws HeroLevelException, InvalidStateException, ManaInvestedException, WizardException{
        // no chaneling prior to spellSurge()
        assertEquals(Wizard.Stance.IDLE, wizard.getCurrentStance());
        // mana before
        int manaBefore = wizard.getManaStore();
        assertEquals(600, manaBefore);

        // test for exception
        assertThrows(InvalidStateException.class, () -> wizard.spellSurge(1));
        // check for unchanged mana
        assertEquals(manaBefore, wizard.getManaStore());
    }

    // Challenge 3: Mana Store Boundary Invariant Under Drain
    static Stream<Arguments> surgeAttempts() {
        return Stream.of(
                // single surge above current manaStore (600)
                Arguments.of("surge exceeding manaStore", new int[]{ 601 }, ManaInvestedException.class),
                // drain 500 (leaves 100), then attempt 200 > 100
                Arguments.of("drain then over-invest", new int[]{ 500, 200 }, ManaInvestedException.class),
                // below MIN_MANA_INVESTED (1)
                Arguments.of("surge below minimum investment", new int[]{ 0 }, ManaInvestedException.class)
        );
    }

    @ParameterizedTest(name = "spellSurge: {0}")
    @MethodSource("surgeAttempts")
    void spellSurgeToNegative(
            String description, int[] surgeAmounts, Class<? extends WizardException> expected
    ) throws Exception {
        Wizard wizard = new Wizard(5, 100);
        wizard.beginChanneling();                       // legal stance
        assertEquals(600, wizard.getManaStore());       // MAX_MANA_STORE

        boolean caught = false;
        for (int amount : surgeAmounts) {
            int before = wizard.getManaStore();
            try {
                wizard.spellSurge(amount);
            } catch (WizardException e) {
                assertEquals(before, wizard.getManaStore(),
                        "Rejected surge must not mutate manaStore: " + description);
                assertEquals(expected, e.getClass(),
                        "Unexpected exception type: " + description);
                caught = true;
                break;
            }
            assertTrue(wizard.getManaStore() >= 0,
                    "manaStore negative after a permitted surge: " + description);
        }
        assertTrue(caught, "Expected guard exception to fire: " + description);
    }
    static Stream<Arguments> castAttempts() {
        return Stream.of(
                // baseManaCost 1000 > manaStore 600, minLevel 1 satisfiable at level 10
                Arguments.of("cast baseManaCost > manaStore", 1000),
                // boundary: 601 drains past zero by exactly 1
                Arguments.of("cast boundary overflow", 601)
        );
    }

    @ParameterizedTest(name = "castSpell: {0}")
    @MethodSource("castAttempts")
    void castSpellToNegative(String description, int baseManaCost)
            throws Exception {
        Wizard wizard = new Wizard(10, 100);
        // Spell(name, baseManaCost, minLevelRequired)
        Spell hugeSpell = new Spell("Overloaded Bolt", baseManaCost, 1);

        assertEquals(600, wizard.getManaStore());
        int before = wizard.getManaStore();

        try {
            wizard.castSpell(hugeSpell);
        } catch (WizardException e) {
            // A correct implementation throws here and leaves manaStore intact.
            assertEquals(before, wizard.getManaStore(),
                    "Rejected cast must not mutate manaStore: " + description);
            return;
        }

        // No exception -> the cast must not have gone negative.
        // FAILS against current source (missing mana sufficiency check).
        assertTrue(wizard.getManaStore() >= 0,
                "castSpell drove manaStore negative (" + wizard.getManaStore()
                        + "): " + description);
    }

// Challenge 4: Liskov Substitution Principle (LSP) Invariant Violation in Archmage
    @Test
    void polymorphicRefTest(){
        Spellcaster wizard = new Archmage(10, 100);
        Archmage arch = (Archmage) wizard;

        arch.toggleBarrier();
        int manaBefore = wizard.getManaStore();
        assertEquals(600, manaBefore);

        // Act: damage far exceeds manaStore
        int damage = manaBefore + 500;       // 1100 >> 600
        wizard.receiveDamage(damage);

        // Assert: superclass invariant manaStore >= 0 must hold
        assertTrue(wizard.getManaStore() >= 0,
                "LSP violation: manaStore dropped to " + wizard.getManaStore()
                        + " (Spellcaster contract requires >= 0)");
    }

    // Challenge 5: The "Yo-Yo Effect" Cascade
    @Test
    void yoYoTest() throws Exception{
        Archmage arch = new Archmage(10, 100);
        arch.beginChanneling();

        assertEquals(600, arch.getManaStore());
        int result = arch.spellSurge(101);
        assertEquals(302, result, "tuning check: expected overcharge*2 = 302");

        assertEquals(600, arch.getManaStore(),
                "rest() cascade refilled mana mid-spellSurge (yo-yo effect)");

        assertEquals(Wizard.Stance.CHANNELING, arch.getCurrentStance(),
                "stance left CHANNELING after rest() cascaded (rest() fault: no IDLE reset)");
    }

    // Challenge 6: Dynamic Binding & Polymorphic Subclass Substitution
    private String runSequence(Wizard w) throws Exception {
        w.beginChanneling();
        w.spellSurge(50);
        w.spellSurge(50);
        w.rest();               // mana -> 600; stance bug leaves it CHANNELING (both types)
        w.beginChanneling();
        w.spellSurge(101);      // Archmage: 302 > 200 -> rest() cascade; Wizard: 151, no cascade
        return "mana=" + w.getManaStore() + ",stance=" + w.getCurrentStance();
    }

    @Test
    void identicalSequenceTest() throws Exception{
        String plainState = runSequence(new Wizard(10, 100));
        String archState  = runSequence(new Archmage(10, 100));

        assertEquals(plainState, archState,
                "LSP divergence:\n  Wizard   : " + plainState + "\n  Archmage : " + archState);
    }

    // Challenge 7: Homogeneous Type Parameter Swap in Inter-Object Messaging
    @Test
    void transferMana_deductsExactAmountFromSender() throws Exception{
        Wizard sender    = new Wizard(10, 100);
        Wizard recipient = new Wizard(10, 100);

        int senderBefore    = sender.getManaStore();     // 600
        int recipientBefore = recipient.getManaStore();  // 600

        sender.transferMana(recipient, /* amount */ 100, /* priorityTier */ 1);

        assertEquals(senderBefore - 100, sender.getManaStore(),
                "Sender lost " + (senderBefore - sender.getManaStore())
                        + " instead of 100 (amount/priorityTier transposed in impl)");
        assertEquals(recipientBefore + 100, recipient.getManaStore(),
                "Recipient gain wrong");
    }

    // Challenge 8: Static Overload Resolution vs. Dynamic Polymorphic Dispatch

    //safe wrapper
    static int safeCast(Wizard w, Object target) throws WizardException {
        if (target instanceof Spell s) {
            return w.castSpell(s);            // statically bound to castSpell(Spell)
        }
        throw new IllegalArgumentException("Unsupported cast target: " + target);
    }
    @Test
    void castSpellCompileTest() throws Exception{

    }
}
