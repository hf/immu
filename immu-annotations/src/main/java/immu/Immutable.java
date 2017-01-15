package immu;

/**
 * All {@link Immu} annotated interface implementations will also extend this interface.
 * <p>
 * Do not extend {@link Immu} or {@link SuperImmu} interfaces with this interface, since
 * this interface is only meant for the interface implementation.
 * <p>
 * You can also use this interface to designate custom immutable object implementations as
 * immutable.
 */
public interface Immutable {

  /**
   * Clears any cached elements in this implementation. Usually {@link Object#hashCode()} and {@link Object#toString()}
   * are cached. Calling this method <strong>may</strong> clear <strong>some</strong>
   * of those cached values and will have no effect on the immutable properties of the object. This
   * method does not block, and the clearing is done optimistically.
   * <p>
   * Only use this method in memory-constricted environments, like Android, in order to not keep large
   * strings, or precomputed values in memory.
   */
  void clear();
}
