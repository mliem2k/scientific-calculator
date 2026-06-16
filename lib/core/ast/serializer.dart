import 'types.dart';

String serialize(List<ASTNode> nodes) {
  return nodes.map(_serializeNode).join('');
}

String _serializeNode(ASTNode node) {
  switch (node) {
    case NumberNode n:
      return n.value;
    case OperatorNode n:
      if (n.op == '×') return '*';
      if (n.op == '÷') return '/';
      return n.op;
    case ConstantNode n:
      return n.name == 'Ans' ? 'ans' : n.name;
    case FractionNode n:
      return '(${serialize(n.numerator)})/(${serialize(n.denominator)})';
    case ExponentNode n:
      return '(${serialize(n.base)})^(${serialize(n.exponent)})';
    case RadicalNode n:
      if (n.degree.isEmpty) {
        return 'sqrt(${serialize(n.radicand)})';
      }
      return 'nthRoot(${serialize(n.radicand)}, ${serialize(n.degree)})';
    case FunctionNode n:
      if (n.name == FunctionName.log) {
        return 'log(${serialize(n.argument)}, 10)';
      }
      if (n.name == FunctionName.ln) {
        return 'log(${serialize(n.argument)})';
      }
      if (n.name == FunctionName.pow10) {
        return '10^(${serialize(n.argument)})';
      }
      if (n.name == FunctionName.exp) {
        return 'e^(${serialize(n.argument)})';
      }
      return '${n.name.name}(${serialize(n.argument)})';
    case FactorialNode n:
      return 'factorial(${serialize(n.operand)})';
    case NcrNode n:
      return 'combinations(${serialize(n.n)}, ${serialize(n.r)})';
    case NprNode n:
      return 'permutations(${serialize(n.n)}, ${serialize(n.r)})';
    case ParenNode n:
      return '(${serialize(n.children)})';
  }
}
