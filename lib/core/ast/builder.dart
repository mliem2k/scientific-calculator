import 'types.dart';

ASTNode _deepCopyNode(ASTNode node) {
  switch (node) {
    case NumberNode n:
      return NumberNode(n.value);
    case OperatorNode n:
      return OperatorNode(n.op);
    case ConstantNode n:
      return ConstantNode(n.name);
    case FractionNode n:
      return FractionNode(_deepCopy(n.numerator), _deepCopy(n.denominator));
    case ExponentNode n:
      return ExponentNode(_deepCopy(n.base), _deepCopy(n.exponent));
    case RadicalNode n:
      return RadicalNode(_deepCopy(n.degree), _deepCopy(n.radicand));
    case FunctionNode n:
      return FunctionNode(n.name, _deepCopy(n.argument));
    case FactorialNode n:
      return FactorialNode(_deepCopy(n.operand));
    case NcrNode n:
      return NcrNode(_deepCopy(n.n), _deepCopy(n.r));
    case NprNode n:
      return NprNode(_deepCopy(n.n), _deepCopy(n.r));
    case ParenNode n:
      return ParenNode(_deepCopy(n.children));
  }
}

List<ASTNode> _deepCopy(List<ASTNode> nodes) {
  return nodes.map(_deepCopyNode).toList();
}

List<ASTNode> _getList(List<ASTNode> root, List<CursorSegment> path) {
  List<ASTNode> cur = root;
  for (final seg in path) {
    final node = cur[seg.nodeIndex];
    cur = _slotOf(node, seg.slot);
  }
  return cur;
}

List<ASTNode> _slotOf(ASTNode node, String slot) {
  switch (node) {
    case FractionNode n:
      if (slot == 'numerator') return n.numerator;
      if (slot == 'denominator') return n.denominator;
    case ExponentNode n:
      if (slot == 'base') return n.base;
      if (slot == 'exponent') return n.exponent;
    case RadicalNode n:
      if (slot == 'degree') return n.degree;
      if (slot == 'radicand') return n.radicand;
    case FunctionNode n:
      if (slot == 'argument') return n.argument;
    case FactorialNode n:
      if (slot == 'operand') return n.operand;
    case NcrNode n:
      if (slot == 'n') return n.n;
      if (slot == 'r') return n.r;
    case NprNode n:
      if (slot == 'n') return n.n;
      if (slot == 'r') return n.r;
    case ParenNode n:
      if (slot == 'children') return n.children;
    default:
      break;
  }
  throw ArgumentError('Unknown slot "$slot" on node ${node.runtimeType}');
}

(List<ASTNode>, Cursor) _spliceInsert(
    List<ASTNode> root, Cursor cursor, ASTNode node) {
  final r = _deepCopy(root);
  _getList(r, cursor.path).insert(cursor.insertAt, node);
  return (r, Cursor(cursor.path, cursor.insertAt + 1));
}

(List<ASTNode>, Cursor, int) _wrapPrev(
    List<ASTNode> root,
    Cursor cursor,
    ASTNode Function(List<ASTNode> prev) makeNode) {
  final r = _deepCopy(root);
  final list = _getList(r, cursor.path);
  final hasPrev = cursor.insertAt > 0;
  final List<ASTNode> prev;
  final int idx;
  if (hasPrev) {
    prev = list.sublist(cursor.insertAt - 1, cursor.insertAt);
    list.removeRange(cursor.insertAt - 1, cursor.insertAt);
    idx = cursor.insertAt - 1;
  } else {
    prev = [];
    idx = cursor.insertAt;
  }
  list.insert(idx, makeNode(prev));
  return (r, Cursor(cursor.path, idx + 1), idx);
}

(List<ASTNode>, Cursor) insertDigit(
    List<ASTNode> root, Cursor cursor, String digit) {
  final r = _deepCopy(root);
  final list = _getList(r, cursor.path);
  if (cursor.insertAt > 0) {
    final prev = list[cursor.insertAt - 1];
    if (prev is NumberNode) {
      prev.value += digit;
      return (r, cursor);
    }
  }
  return _spliceInsert(root, cursor, NumberNode(digit));
}

(List<ASTNode>, Cursor) insertDecimalPoint(List<ASTNode> root, Cursor cursor) {
  final r = _deepCopy(root);
  final list = _getList(r, cursor.path);
  if (cursor.insertAt > 0) {
    final prev = list[cursor.insertAt - 1];
    if (prev is NumberNode) {
      if (!prev.value.contains('.')) prev.value += '.';
      return (r, cursor);
    }
  }
  return _spliceInsert(root, cursor, NumberNode('0.'));
}

(List<ASTNode>, Cursor) insertOperator(
    List<ASTNode> root, Cursor cursor, String op) {
  return _spliceInsert(root, cursor, OperatorNode(op));
}

(List<ASTNode>, Cursor) insertFraction(List<ASTNode> root, Cursor cursor) {
  final (r, c) = _spliceInsert(root, cursor, FractionNode([], []));
  return (
    r,
    Cursor([...c.path, CursorSegment(c.insertAt - 1, 'numerator')], 0)
  );
}

(List<ASTNode>, Cursor) insertExponent(List<ASTNode> root, Cursor cursor) {
  final (r, c, idx) = _wrapPrev(
    root,
    cursor,
    (prev) => ExponentNode(prev, []),
  );
  return (r, Cursor([...c.path, CursorSegment(idx, 'exponent')], 0));
}

(List<ASTNode>, Cursor) insertRadical(
    List<ASTNode> root, Cursor cursor, bool cubeRoot) {
  final degree = cubeRoot ? [NumberNode('3')] : <ASTNode>[];
  final (r, c) = _spliceInsert(root, cursor, RadicalNode(degree, []));
  return (
    r,
    Cursor([...c.path, CursorSegment(c.insertAt - 1, 'radicand')], 0)
  );
}

(List<ASTNode>, Cursor) insertSquare(List<ASTNode> root, Cursor cursor) {
  final (r, c, _) = _wrapPrev(
    root,
    cursor,
    (prev) => ExponentNode(prev, [NumberNode('2')]),
  );
  return (r, c);
}

(List<ASTNode>, Cursor) insertFunction(
    List<ASTNode> root, Cursor cursor, FunctionName name) {
  final (r, c) = _spliceInsert(root, cursor, FunctionNode(name, []));
  return (
    r,
    Cursor([...c.path, CursorSegment(c.insertAt - 1, 'argument')], 0)
  );
}

(List<ASTNode>, Cursor) insertConstant(
    List<ASTNode> root, Cursor cursor, String name) {
  return _spliceInsert(root, cursor, ConstantNode(name));
}

(List<ASTNode>, Cursor) insertFactorial(List<ASTNode> root, Cursor cursor) {
  final (r, c, _) = _wrapPrev(
    root,
    cursor,
    (prev) => FactorialNode(prev),
  );
  return (r, c);
}

(List<ASTNode>, Cursor) insertNcr(List<ASTNode> root, Cursor cursor) {
  final (r, c, idx) = _wrapPrev(
    root,
    cursor,
    (prev) => NcrNode(prev, []),
  );
  return (r, Cursor([...c.path, CursorSegment(idx, 'r')], 0));
}

(List<ASTNode>, Cursor) insertNpr(List<ASTNode> root, Cursor cursor) {
  final (r, c, idx) = _wrapPrev(
    root,
    cursor,
    (prev) => NprNode(prev, []),
  );
  return (r, Cursor([...c.path, CursorSegment(idx, 'r')], 0));
}

(List<ASTNode>, Cursor) insertParen(
    List<ASTNode> root, Cursor cursor, String side) {
  if (side == 'open') {
    final (r, c) = _spliceInsert(root, cursor, ParenNode([]));
    return (
      r,
      Cursor([...c.path, CursorSegment(c.insertAt - 1, 'children')], 0)
    );
  }
  if (cursor.path.isEmpty) return (root, cursor);
  final parentPath = cursor.path.sublist(0, cursor.path.length - 1);
  final lastSeg = cursor.path.last;
  return (root, Cursor(parentPath, lastSeg.nodeIndex + 1));
}

(List<ASTNode>, Cursor) insertExp(List<ASTNode> root, Cursor cursor) {
  final r = _deepCopy(root);
  final list = _getList(r, cursor.path);
  list.insert(cursor.insertAt, OperatorNode('×'));
  list.insert(cursor.insertAt + 1, ExponentNode([NumberNode('10')], []));
  final expIdx = cursor.insertAt + 1;
  return (r, Cursor([...cursor.path, CursorSegment(expIdx, 'exponent')], 0));
}

(List<ASTNode>, Cursor) insertReciprocal(List<ASTNode> root, Cursor cursor) {
  final r = _deepCopy(root);
  final list = _getList(r, cursor.path);
  final hasPrev = cursor.insertAt > 0;
  final List<ASTNode> prev;
  final int idx;
  if (hasPrev) {
    prev = list.sublist(cursor.insertAt - 1, cursor.insertAt);
    list.removeRange(cursor.insertAt - 1, cursor.insertAt);
    idx = cursor.insertAt - 1;
  } else {
    prev = [];
    idx = cursor.insertAt;
  }
  list.insert(idx, FractionNode([NumberNode('1')], prev));
  if (hasPrev) {
    return (r, Cursor(cursor.path, idx + 1));
  }
  return (r, Cursor([...cursor.path, CursorSegment(idx, 'denominator')], 0));
}

(List<ASTNode>, Cursor) deleteCurrent(List<ASTNode> root, Cursor cursor) {
  if (cursor.insertAt == 0 && cursor.path.isEmpty) return (root, cursor);
  final r = _deepCopy(root);
  if (cursor.insertAt == 0) {
    final parentPath = cursor.path.sublist(0, cursor.path.length - 1);
    final lastSeg = cursor.path.last;
    return (r, Cursor(parentPath, lastSeg.nodeIndex));
  }
  final list = _getList(r, cursor.path);
  final node = list[cursor.insertAt - 1];
  if (node is NumberNode && node.value.length > 1) {
    node.value = node.value.substring(0, node.value.length - 1);
    return (r, cursor);
  }
  list.removeAt(cursor.insertAt - 1);
  return (r, Cursor(cursor.path, cursor.insertAt - 1));
}

(List<ASTNode>, Cursor) clearAll() {
  return ([], Cursor(const [], 0));
}

(List<ASTNode>, Cursor) insertCube(List<ASTNode> root, Cursor cursor) {
  final (r, c, _) = _wrapPrev(
    root,
    cursor,
    (prev) => ExponentNode(prev, [NumberNode('3')]),
  );
  return (r, c);
}

(List<ASTNode>, Cursor) insertNthRadical(List<ASTNode> root, Cursor cursor) {
  final (r, c) = _spliceInsert(root, cursor, RadicalNode([], []));
  return (
    r,
    Cursor([...c.path, CursorSegment(c.insertAt - 1, 'degree')], 0)
  );
}

(List<ASTNode>, Cursor) insertMixed(List<ASTNode> root, Cursor cursor) {
  final r = _deepCopy(root);
  final list = _getList(r, cursor.path);
  list.insert(cursor.insertAt, NumberNode('0'));
  list.insert(cursor.insertAt + 1, FractionNode([], []));
  final fracIndex = cursor.insertAt + 1;
  return (
    r,
    Cursor([...cursor.path, CursorSegment(fracIndex, 'numerator')], 0)
  );
}

(List<ASTNode>, Cursor) insertNumberLiteral(
    List<ASTNode> root, Cursor cursor, String value) {
  return _spliceInsert(root, cursor, NumberNode(value));
}
