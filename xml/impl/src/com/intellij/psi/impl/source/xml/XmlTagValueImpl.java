package com.intellij.psi.impl.source.xml;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class XmlTagValueImpl implements XmlTagValue{
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.source.xml.XmlTagValueImpl");

  private final XmlTag myTag;
  private final XmlTagChild[] myElements;
  private volatile XmlText[] myTextElements = null;
  private volatile String myText = null;
  private volatile String myTrimmedText = null;

  public XmlTagValueImpl(@NotNull XmlTagChild[] bodyElements, @NotNull XmlTag tag) {
    myTag = tag;
    myElements = bodyElements;
  }

  @NotNull
  public XmlTagChild[] getChildren() {
    return myElements;
  }

  @NotNull
  public XmlText[] getTextElements() {
    XmlText[] textElements = myTextElements;
    if(textElements != null) return textElements;
    final List<XmlText> textElementsList = new ArrayList<XmlText>();
    for (final XmlTagChild element : myElements) {
      if (element instanceof XmlText) textElementsList.add((XmlText)element);
    }
    return myTextElements = textElementsList.toArray(new XmlText[textElementsList.size()]);
  }

  @NotNull
  public String getText() {
    String text = myText;
    if(text != null) return text;
    final StringBuilder consolidatedText = new StringBuilder();
    for (final XmlTagChild element : myElements) {
      consolidatedText.append(element.getText());
    }
    return myText = consolidatedText.toString();
  }

  @NotNull
  public TextRange getTextRange() {
    if(myElements.length == 0){
      final ASTNode child = XmlChildRole.START_TAG_END_FINDER.findChild( (ASTNode)myTag);
      if(child != null)
        return new TextRange(child.getStartOffset() + 1, child.getStartOffset() + 1);
      return new TextRange(myTag.getTextRange().getEndOffset(), myTag.getTextRange().getEndOffset());
    }
    return new TextRange(myElements[0].getTextRange().getStartOffset(), myElements[myElements.length - 1].getTextRange().getEndOffset());
  }

  @NotNull
  public String getTrimmedText() {
    String trimmedText = myTrimmedText;
    if(trimmedText != null) return trimmedText;

    final StringBuilder consolidatedText = new StringBuilder();
    final XmlText[] textElements = getTextElements();
    for (final XmlText textElement : textElements) {
      consolidatedText.append(textElement.getValue());
    }
    return myTrimmedText = consolidatedText.toString().trim();
  }

  public void setText(String value) {
    try {
      XmlText text = null;
      if (StringUtil.isNotEmpty(value)) {
        final XmlText[] texts = getTextElements();
        if (texts.length == 0) {
          text = (XmlText)myTag.add(XmlElementFactory.getInstance(myTag.getProject()).createDisplayText("x"));
        } else {
          text = texts[0];
        }
        text.setValue(value);
      }

      if(myElements.length > 0){
        for (final XmlTagChild child : myElements) {
          if (child != text) {
            child.delete();
          }
        }
      }
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }
}
