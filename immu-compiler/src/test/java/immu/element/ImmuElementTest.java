package immu.element;

import immu.element.predicate.ImmuPredicate;
import org.junit.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;

public class ImmuElementTest {

  private static final class Elem extends ImmuElement {

    public Elem(Element element) {
      super(element);
    }

    @Override
    public List<ImmuPredicate.Result> validate(ProcessingEnvironment environment) {
      return Collections.emptyList();
    }
  }

  @Test
  public void equals_sameElement() throws Exception {
    final Element element = mock(Element.class);

    final ImmuElement elementA = new Elem(element);
    final ImmuElement elementB = new Elem(element);


    assertEquals(elementA, elementA);
    assertEquals(elementB, elementB);
    assertEquals(elementA, elementB);
    assertEquals(elementB, elementA);
  }

  @Test
  public void equals_differentElement() throws Exception {
    final ImmuElement elementA = new Elem(mock(Element.class));
    final ImmuElement elementB = new Elem(mock(Element.class));

    assertFalse(elementA.equals(null));
    assertFalse(elementB.equals(null));
    assertFalse(elementA.equals("hello"));
    assertFalse(elementB.equals(new Object()));
    assertNotEquals(elementA, elementB);
    assertNotEquals(elementB, elementA);
  }

}
