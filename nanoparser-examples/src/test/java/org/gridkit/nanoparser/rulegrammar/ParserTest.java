package org.gridkit.nanoparser.rulegrammar;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.nanoparser.NanoParser;
import org.gridkit.nanoparser.ParserException;
import org.gridkit.nanoparser.SourceReader;
import org.gridkit.nanoparser.rulegrammar.AST.Lit;
import org.gridkit.nanoparser.rulegrammar.AST.Statement;
import org.gridkit.nanoparser.rulegrammar.AST.Var;
import org.junit.Rule;
import org.junit.Test;

public class ParserTest {

    @Rule
    public ParserTestHelper helper = new ParserTestHelper();
    
    NanoParser<Void> parser = new NanoParser<Void>(new RuleParser(), RuleParser.MAIN_GRAMMAR);
    
    
/* TESTDATA

a(X) :- true

{
  implication: true,
  lhs: {
    params: [
      {
        variable: X
      }
    ],
    symbol: a
  },
  rhs: {
    functor: {
      params: [],
      symbol: true
    }
  }
}
*/
    @Test
    public void parse_simple_rule_true() {
        test_ast();
    }

    /* TESTDATA

a(X, Y) :- true

{
  implication: true,
  lhs: {
    params: [
      {
        variable: X
      },
      {
        variable: Y
      }
    ],
    symbol: a
  },
  rhs: {
    functor: {
      params: [],
      symbol: true
    }
  }
}     
     */
    @Test
    public void parse_two_args_simple_rule() {
        test_ast();
    }

    /* TESTDATA

a(X) :- b(X, Y), c(Y)


{
  implication: true,  
  lhs: {
    params: [
      {
        variable: X
      }
    ],
    symbol: a
  },
  rhs: {
    binary: {
      a: {
        functor: {
          params: [
            {
              variable: X
            },
            {
              variable: Y
            }
          ],
          symbol: b
        }
      },
      b: {
        functor: {
          params: [
            {
              variable: Y
            }
          ],
          symbol: c
        }
      },
      disjunction: true
    }
  }
}
     */
    @Test
    public void parse_simple_two_right_side_rules() {
        test_ast();
    }

    /* TESTDATA

a(X) :- (b(X, Y), c(Y))


{
  implication: true,  
  lhs: {
    params: [
      {
        variable: X
      }
    ],
    symbol: a
  },
  rhs: {
    binary: {
      a: {
        functor: {
          params: [
            {
              variable: X
            },
            {
              variable: Y
            }
          ],
          symbol: b
        }
      },
      b: {
        functor: {
          params: [
            {
              variable: Y
            }
          ],
          symbol: c
        }
      },
      disjunction: true
    }
  }
}
     */
    @Test
    public void parse_simple_eclosure_two_right_side_rules() {
        test_ast();
    }
    
    /* TESTDATA

a(X) :- lit, b(X, Y), c(Y)


{
  implication: true,
  lhs: {
    params: [
      {
        variable: X
      }
    ],
    symbol: a
  },
  rhs: {
    binary: {
      a: {
        binary: {
          a: {
            functor: {
              params: [],
              symbol: lit
            }
          },
          b: {
            functor: {
              params: [
                {
                  variable: X
                },
                {
                  variable: Y
                }
              ],
              symbol: b
            }
          },
          disjunction: true
        }
      },
      b: {
        functor: {
          params: [
            {
              variable: Y
            }
          ],
          symbol: c
        }
      },
      disjunction: true
    }
  }
}
     */
    @Test
    public void parse_simple_three_right_side_rules() {
        test_ast();
    }

    /* TESTDATA

a(X) :- eq(X, 10)


{
  implication: true,
  lhs: {
    params: [
      {
        variable: X
      }
    ],
    symbol: a
  },
  rhs: {
    functor: {
      params: [
        {
          variable: X
        },
        {
          literal: {
            number: 10
          }
        }
      ],
      symbol: eq
    }
  }
}
     */
    @Test
    public void parse_int_literal() {
        test_ast();
    }

    /* TESTDATA

a(X) :- eq(X, 10.1)


{
  implication: true,
  lhs: {
    params: [
      {
        variable: X
      }
    ],
    symbol: a
  },
  rhs: {
    functor: {
      params: [
        {
          variable: X
        },
        {
          literal: {
            number: 10.1
          }
        }
      ],
      symbol: eq
    }
  }
}
     */
    @Test
    public void parse_float_literal() {
        test_ast();
    }

    /* TESTDATA

a(X) :- eq(X, "abc")


{
  implication: true,
  lhs: {
    params: [
      {
        variable: X
      }
    ],
    symbol: a
  },
  rhs: {
    functor: {
      params: [
        {
          variable: X
        },
        {
          literal: {
            string: abc
          }
        }
      ],
      symbol: eq
    }
  }
}
     */
    @Test
    public void parse_string_literal() {
        test_ast();
    }

    /* TESTDATA

a(X) :- eq(X, "\"abc\"")


{
  implication: true,
  lhs: {
    params: [
      {
        variable: X
      }
    ],
    symbol: a
  },
  rhs: {
    functor: {
      params: [
        {
          variable: X
        },
        {
          literal: {
            string: "abc"
          }
        }
      ],
      symbol: eq
    }
  }
}
     */
    @Test
    public void parse_escaped_string_literal() {
        test_ast();
    }

    /* TESTDATA

a(f(A, B)) :- true


{
  implication: true,
  lhs: {
    params: [
      {
        functor: {
          params: [
            {
              variable: A
            },
            {
              variable: B
            }
          ],
          symbol: f
        }
      }
    ],
    symbol: a
  },
  rhs: {
    functor: {
      params: [],
      symbol: true
    }
  }
}
     */
    @Test
    public void parse_lhs_pattern_matching() {
        test_ast();
    }
    
    /* TESTDATA

a(A) :- 
 is_list(A),
 true.
is_list(A) := instance_of(A, "java.util.List").


{
  implication: true,
  lhs: {
    params: [
      {
        variable: A
      }
    ],
    symbol: a
  },
  rhs: {
    binary: {
      a: {
        functor: {
          params: [
            {
              variable: A
            }
          ],
          symbol: is_list
        }
      },
      b: {
        functor: {
          params: [],
          symbol: true
        }
      },
      disjunction: true
    }
  }
}
{
  invariant: true,
  lhs: {
    params: [
      {
        variable: A
      }
    ],
    symbol: is_list
  },
  rhs: {
    functor: {
      params: [
        {
          variable: A
        },
        {
          literal: {
            string: java.util.List
          }
        }
      ],
      symbol: instance_of
    }
  }
}     
     */
    @Test
    public void parse_multiple_rules() {
        test_ast();
    }

    protected void test_ast() {
        try {
            helper.verifyAST(parseStatements(helper.getParseString()));
        }
        catch(ParserException e) {
            System.out.println(e.formatVerboseErrorMessage());;
            throw e;
        }
    }
    
    protected Statement[] parseStatements(String text) {
        List<Statement> result = new ArrayList<Statement>();
        SourceReader reader = new SourceReader(text);
        while(!reader.endOfStream()) {
            Statement stm = parser.parseNext(null, Statement.class, reader);
            if (stm != null) {
                result.add(stm);
            }
        }
        
        return result.toArray(new Statement[0]);
    }
    
    public String format(Object obj) throws IllegalArgumentException, IllegalAccessException {        
        StringBuilder sb = new StringBuilder();
        format(obj, sb);
        return sb.toString();
    }

    private void format(Object obj, StringBuilder sb) throws IllegalArgumentException, IllegalAccessException {
        if (obj instanceof Lit) {
            sb.append(((Lit) obj).body);
        }
        else if (obj instanceof Var) {
            sb.append(((Var) obj).body);
        }
        else if (obj instanceof String) {
            sb.append(obj);
        }
        else if (obj instanceof Boolean) {
            sb.append(obj);
        }
        else if (obj instanceof Number) {
            sb.append(obj);
        }
        else if (obj.getClass().isArray()) {
            int l = Array.getLength(obj);
            sb.append("[");
            boolean fisrt = true;
            for(int i = 0; i != l; ++i) {
                if (!fisrt) {
                    sb.append(", ");
                }
                format(Array.get(obj, i), sb);
            }
            sb.append("]");
        }
        else {
            Class<?> c = obj.getClass();
            sb.append("{");
            boolean fisrt = true;
            for(Field f: c.getFields()) {
                Object v = f.get(obj);
                if (v != null) {
                    if (!fisrt) {
                        sb.append(", ");
                    }
                    fisrt = false;
                    sb.append(f.getName()).append(": ");
                    format(v, sb);
                }
            }
            sb.append("}");
        }
    }    
}
