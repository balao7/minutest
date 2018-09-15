package com.oneeyedmen.minutest

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import kotlin.streams.asSequence


object SpecAndFixturesTests {

    data class Fixture(var thing: String)

    @TestFactory fun `with fixtures`() = context<Fixture> {

        fixture { Fixture("banana") }

        test("can mutate fixture without affecting following tests") {
            thing = "kumquat"
            assertEquals("kumquat", thing)
        }

        test("previous test did not affect me") {
            assertEquals("banana", thing)
        }

        context("sub-context inheriting fixture") {
            test("has the fixture from its parent") {
                assertEquals("banana", thing)
            }
        }

        context("sub-context overriding fixture") {
            fixture { Fixture("apple") }

            test("does not have the fixture from its parent") {
                assertEquals("apple", thing)
            }
        }

        context("sub-context replacing fixture") {
            replaceFixture { Fixture("green $thing") }

            test("sees the replaced fixture") {
                assertEquals("green banana", thing)
            }
        }

        context("sub-context modifying fixture") {
            modifyFixture { thing += "s" }

            modifyFixture { thing = "green $thing" }

            test("sees the modified fixture") {
                assertEquals("green bananas", thing)
            }

            context("sub-contexts see parent mods") {
                modifyFixture { thing = "we have no $thing" }

                test("sees the modified fixture") {
                    assertEquals("we have no green bananas", thing)
                }
            }
        }

        context("sanity check") {
            test("still not changed my context") {
                assertEquals("banana", thing)
            }
        }
    }

    @TestFactory fun `dynamic generation`() = context<Fixture> {
        fixture { Fixture("banana") }

        context("same fixture for each") {
            (1..3).forEach { i ->
                test("test for $i") {}
            }
        }

        context("modify fixture for each test") {
            (1..3).forEach { i ->
                context("banana count $i") {
                    replaceFixture{ Fixture("$i $thing") }
                    test("test for $i") {
                        assertEquals("$i banana", thing)
                    }
                }
            }
        }
    }

    @TestFactory fun `test transforms`() = context<Fixture> {
        fixture { Fixture("banana") }

        context("transform actual node") {
            modifyTests {
                when (it) {
                    is MinuTest -> SingleTest(it.name) {}
                    else -> it
                }
            }
            test("transform can ignore test") {
                fail("Shouldn't get here")
            }
        }
    }

    @TestFactory fun `no fixture`() = context<Unit> {
        test("I need not have a fixture") {
            assertNotNull("banana")
        }
    }

    @Test fun `no fixture when one is needed`() {
        val tests: List<DynamicNode> = context<Fixture> {
            test("I report not having a fixture") {
                assertEquals("banana", thing)
            }
        }
        assertThrows<IllegalStateException> {
            ((tests.first() as DynamicContainer).children.asSequence().first() as DynamicTest).executable.execute()
        }
    }
}