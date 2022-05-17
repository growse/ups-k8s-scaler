/**
 * A special type of Either where both sides are subtypes of a common type (C)
 * Has a function to just return whichever side happens to exist as a C
 */
sealed class Either<C, A : C, B : C> {
    class Left<C, A : C, B : C>(val left: A) : Either<C, A, B>()
    class Right<C, A : C, B : C>(val right: B) : Either<C, A, B>()

    fun it() = when (this) {
        is Left -> this.left
        is Right -> this.right
    }
}