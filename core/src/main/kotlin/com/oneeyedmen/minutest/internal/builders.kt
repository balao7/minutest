package com.oneeyedmen.minutest.internal

import com.oneeyedmen.minutest.Context
import com.oneeyedmen.minutest.TestTransform
import kotlin.reflect.KType

internal interface NodeBuilder<F> {
    fun toTestNode(parent: ParentContext<F>): TestNode
}

internal class ContextBuilder<PF, F>(
    private val name: String,
    private val type: KType,
    private var fixtureFactory: (PF.() -> F)?,
    private var explicitFixtureFactory: Boolean
) : Context<PF, F>(), NodeBuilder<PF> {

    private val children = mutableListOf<NodeBuilder<F>>()
    private val operations = Operations<F>()

    override fun fixture(factory: PF.() -> F) {
        if (explicitFixtureFactory)
            throw IllegalStateException("Fixture already set in context \"$name\"")
        fixtureFactory = factory
        explicitFixtureFactory = true
    }

    override fun before(operation: F.() -> Unit) {
        operations.befores.add(operation)
    }

    override fun after(operation: F.() -> Unit) {
        operations.afters.add(operation)
    }

    override fun test_(name: String, f: F.() -> F) {
        children.add(TestBuilder(name, f))
    }

    override fun context(name: String, builder: Context<F, F>.() -> Unit) {
        createSubContext(name, type, { this }, false, builder)
    }

    override fun <G> createSubContext(
        name: String,
        type: KType,
        fixtureFactory: (F.() -> G)?,
        explicitFixtureFactory: Boolean,
        builder: Context<F, G>.() -> Unit
    ) {
        children.add(ContextBuilder(name, type, fixtureFactory, explicitFixtureFactory).apply(builder))
    }

    override fun addTransform(transform: TestTransform<F>) {
        operations.transforms += transform
    }

    override fun toTestNode(parent: ParentContext<PF>): MiContext<PF, F> {
        val fixtureFactory = fixtureFactory ?: error("Fixture has not been set in context \"$name\"")
        return MiContext(name, parent, emptyList(), fixtureFactory, operations).let { context ->
            // nastiness to set up parent child in immutable nodes
            context.copy(children = this.children.map { child -> child.toTestNode(context) })
        }
    }
}

internal data class TestBuilder<F>(val name: String, val f: F.() -> F) : NodeBuilder<F> {
    override fun toTestNode(parent: ParentContext<F>) = MinuTest(name, parent, f)
}