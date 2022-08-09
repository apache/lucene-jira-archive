class Lev2ParametricDescription {
  private final int w;
  
  //@Override
  int transition(int absState, int position, int vector) {
    
    // decode absState -> state, offset
    int state = absToState[absState];
    int offset = absState - stateToAbs[state];
    assert offset >= 0;
    
    // null state should never be passed in
    assert state != -1;
    if (position == w) {
      switch(state) {
        case 0: // (0, 0)
          state = 1; // (0, 1)
          break;
        case 1: // (0, 1)
          state = 2; // (0, 2)
          break;
        case 2: // (0, 2)
          state = -1; // 
          break;
      }
    } else if (position == w-1) {
      switch(vector) {
        case 0: // <0>
          switch(state) {
            case 0: // (0, 0)
              state = 3; // (0, 1), (1, 1)
              break;
            case 3: // (0, 1), (1, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 1: // <1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
      }
    } else if (position == w-2) {
      switch(vector) {
        case 0: // <0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 3; // (0, 1), (1, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 9: // (0, 2), (2, 1)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 1: // <0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 2: // <1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 3: // <1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
      }
    } else if (position == w-3) {
      switch(vector) {
        case 0: // <0,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 3; // (0, 1), (1, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = -1; // 
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 1: // <0,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 12; // (0, 1), (1, 1), (3, 2)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 2: // <0,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 9; // (0, 2), (2, 1)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 3: // <0,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 9; // (0, 2), (2, 1)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 4: // <1,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 5: // <1,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 6: // <1,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 7: // <1,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 5; // (0, 1), (1, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
      }
    } else if (position == w-4) {
      switch(vector) {
        case 0: // <0,0,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 3; // (0, 1), (1, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = -1; // 
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = -1; // 
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 1: // <0,0,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 3; // (0, 1), (1, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = -1; // 
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 2: // <0,0,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 12; // (0, 1), (1, 1), (3, 2)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = -1; // 
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 3: // <0,0,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 12; // (0, 1), (1, 1), (3, 2)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 23; // (0, 2), (1, 2), (3, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 4: // <0,1,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 9; // (0, 2), (2, 1)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 5: // <0,1,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 25; // (0, 2), (2, 1), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 6: // <0,1,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 9; // (0, 2), (2, 1)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 7: // <0,1,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 25; // (0, 2), (2, 1), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 21; // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 8: // <1,0,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 9: // <1,0,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 10: // <1,0,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 11: // <1,0,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 13; // (0, 1), (2, 2), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 12: // <1,1,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 13: // <1,1,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 12; // (0, 1), (1, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 14: // <1,1,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 5; // (0, 1), (1, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 15: // <1,1,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 5; // (0, 1), (1, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 12; // (0, 1), (1, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 13; // (0, 1), (2, 2), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
      }
    } else {
      switch(vector) {
        case 0: // <0,0,0,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 3; // (0, 1), (1, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = -1; // 
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = -1; // 
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 1: // <0,0,0,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 3; // (0, 1), (1, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = -1; // 
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = -1; // 
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 2: // <0,0,0,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 3; // (0, 1), (1, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = -1; // 
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 3: // <0,0,0,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 3; // (0, 1), (1, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 4;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = -1; // 
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 4;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 4;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 4: // <0,0,1,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 12; // (0, 1), (1, 1), (3, 2)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = -1; // 
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 5: // <0,0,1,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 12; // (0, 1), (1, 1), (3, 2)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 4; // (0, 2), (1, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 3;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 3;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = -1; // 
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 6: // <0,0,1,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 12; // (0, 1), (1, 1), (3, 2)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 23; // (0, 2), (1, 2), (3, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = -1; // 
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 7: // <0,0,1,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 12; // (0, 1), (1, 1), (3, 2)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 23; // (0, 2), (1, 2), (3, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 4; // (0, 2), (1, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 3;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 3;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 4;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 4: // (0, 2), (1, 2)
              state = -1; // 
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 8: // <0,1,0,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 9; // (0, 2), (2, 1)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 9: // <0,1,0,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 9; // (0, 2), (2, 1)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = -1; // 
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 10: // <0,1,0,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 25; // (0, 2), (2, 1), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = -1; // 
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 11: // <0,1,0,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 25; // (0, 2), (2, 1), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 2;
              break;
            case 9: // (0, 2), (2, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 4;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 10: // (0, 2), (2, 2)
              state = -1; // 
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 12: // <0,1,1,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 9; // (0, 2), (2, 1)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = -1; // 
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 13: // <0,1,1,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 9; // (0, 2), (2, 1)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 20: // (0, 2), (3, 2)
              state = -1; // 
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 14: // <0,1,1,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 25; // (0, 2), (2, 1), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 21; // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = -1; // 
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 15: // <0,1,1,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 5; // (0, 1), (1, 1), (2, 1)
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 25; // (0, 2), (2, 1), (4, 2)
              break;
            case 3: // (0, 1), (1, 1)
              state = 9; // (0, 2), (2, 1)
              break;
            case 7: // (0, 1), (2, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 21; // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              break;
            case 8: // (0, 1), (2, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              break;
            case 14: // (0, 1), (3, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              break;
            case 1: // (0, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 2;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 2;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 2;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 2;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 2;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 2;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 2;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 2;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 2;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 3;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 3;
              break;
            case 9: // (0, 2), (2, 1)
              state = 1; // (0, 1)
              offset += 3;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 3;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 3;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 3;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 3;
              break;
            case 19: // (0, 2), (3, 1)
              state = 1; // (0, 1)
              offset += 4;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 4;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 4;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 5;
              break;
            case 2: // (0, 2)
              state = -1; // 
              break;
          }
          break;
        case 16: // <1,0,0,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 17: // <1,0,0,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 18: // <1,0,0,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 19: // <1,0,0,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 28; // (0, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 28; // (0, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 28; // (0, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 28; // (0, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 20: // <1,0,1,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 21: // <1,0,1,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 27; // (0, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 27; // (0, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 25; // (0, 2), (2, 1), (4, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 27; // (0, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 27; // (0, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 22: // <1,0,1,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 13; // (0, 1), (2, 2), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 23: // <1,0,1,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 13; // (0, 1), (2, 2), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 26; // (0, 2), (2, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 27; // (0, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 28; // (0, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 25; // (0, 2), (2, 1), (4, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 26; // (0, 2), (2, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 27; // (0, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 28; // (0, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 24: // <1,1,0,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 25: // <1,1,0,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 26: // <1,1,0,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 12; // (0, 1), (1, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 27: // <1,1,0,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 12; // (0, 1), (1, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 23; // (0, 2), (1, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 23; // (0, 2), (1, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 28; // (0, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 28; // (0, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 28: // <1,1,1,0,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 5; // (0, 1), (1, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 29: // <1,1,1,0,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 5; // (0, 1), (1, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 25; // (0, 2), (2, 1), (4, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 27; // (0, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 27; // (0, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 30: // <1,1,1,1,0>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 5; // (0, 1), (1, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 12; // (0, 1), (1, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 13; // (0, 1), (2, 2), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
        case 31: // <1,1,1,1,1>
          switch(state) {
            case 0: // (0, 0)
              state = 0; // (0, 0)
              offset += 1;
              break;
            case 5: // (0, 1), (1, 1), (2, 1)
              state = 5; // (0, 1), (1, 1), (2, 1)
              offset += 1;
              break;
            case 12: // (0, 1), (1, 1), (3, 2)
              state = 12; // (0, 1), (1, 1), (3, 2)
              offset += 1;
              break;
            case 3: // (0, 1), (1, 1)
              state = 3; // (0, 1), (1, 1)
              offset += 1;
              break;
            case 7: // (0, 1), (2, 1)
              state = 7; // (0, 1), (2, 1)
              offset += 1;
              break;
            case 13: // (0, 1), (2, 2), (3, 2)
              state = 13; // (0, 1), (2, 2), (3, 2)
              offset += 1;
              break;
            case 8: // (0, 1), (2, 2)
              state = 8; // (0, 1), (2, 2)
              offset += 1;
              break;
            case 14: // (0, 1), (3, 2)
              state = 14; // (0, 1), (3, 2)
              offset += 1;
              break;
            case 1: // (0, 1)
              state = 1; // (0, 1)
              offset += 1;
              break;
            case 21: // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              state = 21; // (0, 2), (1, 2), (2, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 11: // (0, 2), (1, 2), (2, 2), (3, 2)
              state = 11; // (0, 2), (1, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 22: // (0, 2), (1, 2), (2, 2), (4, 2)
              state = 22; // (0, 2), (1, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 6: // (0, 2), (1, 2), (2, 2)
              state = 6; // (0, 2), (1, 2), (2, 2)
              offset += 1;
              break;
            case 15: // (0, 2), (1, 2), (3, 1)
              state = 15; // (0, 2), (1, 2), (3, 1)
              offset += 1;
              break;
            case 23: // (0, 2), (1, 2), (3, 2), (4, 2)
              state = 23; // (0, 2), (1, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 16: // (0, 2), (1, 2), (3, 2)
              state = 16; // (0, 2), (1, 2), (3, 2)
              offset += 1;
              break;
            case 24: // (0, 2), (1, 2), (4, 2)
              state = 24; // (0, 2), (1, 2), (4, 2)
              offset += 1;
              break;
            case 4: // (0, 2), (1, 2)
              state = 4; // (0, 2), (1, 2)
              offset += 1;
              break;
            case 17: // (0, 2), (2, 1), (3, 1)
              state = 17; // (0, 2), (2, 1), (3, 1)
              offset += 1;
              break;
            case 25: // (0, 2), (2, 1), (4, 2)
              state = 25; // (0, 2), (2, 1), (4, 2)
              offset += 1;
              break;
            case 9: // (0, 2), (2, 1)
              state = 9; // (0, 2), (2, 1)
              offset += 1;
              break;
            case 26: // (0, 2), (2, 2), (3, 2), (4, 2)
              state = 26; // (0, 2), (2, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 18: // (0, 2), (2, 2), (3, 2)
              state = 18; // (0, 2), (2, 2), (3, 2)
              offset += 1;
              break;
            case 27: // (0, 2), (2, 2), (4, 2)
              state = 27; // (0, 2), (2, 2), (4, 2)
              offset += 1;
              break;
            case 10: // (0, 2), (2, 2)
              state = 10; // (0, 2), (2, 2)
              offset += 1;
              break;
            case 19: // (0, 2), (3, 1)
              state = 19; // (0, 2), (3, 1)
              offset += 1;
              break;
            case 28: // (0, 2), (3, 2), (4, 2)
              state = 28; // (0, 2), (3, 2), (4, 2)
              offset += 1;
              break;
            case 20: // (0, 2), (3, 2)
              state = 20; // (0, 2), (3, 2)
              offset += 1;
              break;
            case 29: // (0, 2), (4, 2)
              state = 29; // (0, 2), (4, 2)
              offset += 1;
              break;
            case 2: // (0, 2)
              state = 2; // (0, 2)
              offset += 1;
              break;
          }
          break;
      }
    }
    
    if (state == -1) {
      // null state
      return -1;
    } else {
      // translate back to abs
      return stateToAbs[state] + offset;
    }
  }
  
  // state map
  //   0 -> [(0, 0)]
  //   1 -> [(0, 1)]
  //   2 -> [(0, 2)]
  //   3 -> [(0, 1), (1, 1)]
  //   4 -> [(0, 2), (1, 2)]
  //   5 -> [(0, 1), (1, 1), (2, 1)]
  //   6 -> [(0, 2), (1, 2), (2, 2)]
  //   7 -> [(0, 1), (2, 1)]
  //   8 -> [(0, 1), (2, 2)]
  //   9 -> [(0, 2), (2, 1)]
  //   10 -> [(0, 2), (2, 2)]
  //   11 -> [(0, 2), (1, 2), (2, 2), (3, 2)]
  //   12 -> [(0, 1), (1, 1), (3, 2)]
  //   13 -> [(0, 1), (2, 2), (3, 2)]
  //   14 -> [(0, 1), (3, 2)]
  //   15 -> [(0, 2), (1, 2), (3, 1)]
  //   16 -> [(0, 2), (1, 2), (3, 2)]
  //   17 -> [(0, 2), (2, 1), (3, 1)]
  //   18 -> [(0, 2), (2, 2), (3, 2)]
  //   19 -> [(0, 2), (3, 1)]
  //   20 -> [(0, 2), (3, 2)]
  //   21 -> [(0, 2), (1, 2), (2, 2), (3, 2), (4, 2)]
  //   22 -> [(0, 2), (1, 2), (2, 2), (4, 2)]
  //   23 -> [(0, 2), (1, 2), (3, 2), (4, 2)]
  //   24 -> [(0, 2), (1, 2), (4, 2)]
  //   25 -> [(0, 2), (2, 1), (4, 2)]
  //   26 -> [(0, 2), (2, 2), (3, 2), (4, 2)]
  //   27 -> [(0, 2), (2, 2), (4, 2)]
  //   28 -> [(0, 2), (3, 2), (4, 2)]
  //   29 -> [(0, 2), (4, 2)]
  
  private final static int[] stateSizes = new int[] {1,1,1,2,2,3,3,2,2,2,2,4,3,3,2,3,3,3,3,2,2,5,4,4,3,3,4,3,3,2};
  private final static int[] minErrors = new int[] {0,1,2,0,1,-1,0,-1,0,-1,0,-1,-1,-1,-1,-2,-1,-2,-1,-2,-1,-2,-2,-2,-2,-2,-2,-2,-2,-2};
  private final int[] stateToAbs;
  private final int[] absToState;
  
  public Lev2ParametricDescription(int w) {
    this.w = w;
    stateToAbs = new int[30];
    absToState = new int[w*80];
    int upto = 0;
    for(int i=0;i<stateSizes.length;i++) {
      stateToAbs[i] = upto;
      for(int j=0;j<(w*stateToAbs.length);j++) {
        absToState[upto++] = i;
      }
    }
  }
  
  //@Override
  public int size() {
    return absToState.length;
  }
  
  //@Override
  public int getPosition(int absState) {
    return absToState[absState];
  }
  
  //@Override
  public boolean isAccept(int absState) {
    // decode absState -> state, offset
    int state = absToState[absState];
    int offset = absState - stateToAbs[state];
    assert offset >= 0;
    return w - offset + minErrors[state] <= 2;
  }
}
