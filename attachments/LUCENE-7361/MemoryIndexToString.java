
  /**
   * Returns a String representation of the index data for debugging purposes.
   * 
   * @return the string representation
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(256);
    result.append("Field list: " + fields.keySet() + "\n");
    //TODO reader.getFieldInfos().toString(); FieldInfos has no toString() nor does FieldInfo
    try (MemoryIndexReader reader = new MemoryIndexReader()) {
      result.append("reader.fields().terms():\n");
      for (String fieldName : reader.fields()) {
        result.append(fieldName).append(": ");
        Terms terms = reader.terms(fieldName);
        if (terms != null) {
          writeTermsDetails(terms, result);
        }
      }
      //TODO append info for pointValues, docValues, norms
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result.toString();
  }

  private void writeTermsDetails(Terms terms, Appendable result) throws IOException {
    // summary stats
    result.append("terms=" + terms.size() + " sumTotalTermFreq=" + terms.getSumTotalTermFreq() + "\n");
    TermsEnum termsEnum = terms.iterator();
    PostingsEnum postingsEnum = null;//re-use
    // Terms:
    for (BytesRef term; ((term = termsEnum.next()) != null);) {
      result.append('\t').append('\'').append(term.utf8ToString()).append('\'');
      result.append(" df").append(String.valueOf(termsEnum.docFreq()));
      // Doc IDs:
      postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.ALL);
      // TODO in general case can be null if offsets/payloads aren't there
      for (int docId; ((docId = postingsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS);) {
        result.append(" d").append(String.valueOf(docId));

        // Freq:
        if (terms.hasFreqs()) {
          int freq = postingsEnum.freq();
          result.append("pf").append(String.valueOf(freq));

          // Positions (with offsets & payloads if applicable)
          if (terms.hasPositions()) {
            result.append("p[");

            for (int pIdx = 0; pIdx < freq; pIdx++) {
              if (pIdx != 0) {
                result.append(",");
              }
              result.append(String.valueOf(postingsEnum.nextPosition()));
              if (terms.hasOffsets()) {
                result.append('o').append(String.valueOf(postingsEnum.startOffset()))
                    .append('-').append(String.valueOf(postingsEnum.endOffset()));
              }
              if (terms.hasPayloads()) {
                result.append('y');
                BytesRef payload = postingsEnum.getPayload();
                if (payload != null) {
                  result.append(payload.toString());
                }
              }
            }

            result.append(']');
          }
        }
      }

      result.append('\n');
    }
  }