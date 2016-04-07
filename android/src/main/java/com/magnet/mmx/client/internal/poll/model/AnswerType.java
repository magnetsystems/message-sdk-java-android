/**
 * File generated by Magnet Magnet Lang Tool on Apr 5, 2016 4:54:27 PM
 * @see {@link http://developer.magnet.com}
 */

package com.magnet.mmx.client.internal.poll.model;

public class AnswerType {

  
  
  private String id;

  
  private String description;

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }


  /**
  * Builder for AnswerType
  **/
  public static class AnswerTypeBuilder {
    private AnswerType toBuild = new AnswerType();

    public AnswerTypeBuilder() {
    }

    public AnswerType build() {
      return toBuild;
    }

    public AnswerTypeBuilder id(String value) {
      toBuild.id = value;
      return this;
    }

    public AnswerTypeBuilder description(String value) {
      toBuild.description = value;
      return this;
    }
  }
}
