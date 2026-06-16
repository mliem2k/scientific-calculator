import 'dart:math';
import 'types.dart';

List<ASTNode>? _getSlot(ASTNode node, String slot) {
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
  return null;
}

List<ASTNode> _getList(List<ASTNode> root, List<CursorSegment> path) {
  List<ASTNode> cur = root;
  for (final seg in path) {
    cur = _getSlot(cur[seg.nodeIndex], seg.slot)!;
  }
  return cur;
}

List<ASTNode> _parentList(List<ASTNode> root, Cursor cursor) {
  return _getList(root, cursor.path.sublist(0, cursor.path.length - 1));
}

Cursor moveCursorRight(List<ASTNode> root, Cursor cursor) {
  final list = _getList(root, cursor.path);
  if (cursor.insertAt < list.length) {
    final next = list[cursor.insertAt];
    if (next is FractionNode) {
      return Cursor(
          [...cursor.path, CursorSegment(cursor.insertAt, 'numerator')], 0);
    }
    if (next is ExponentNode) {
      return Cursor(
          [...cursor.path, CursorSegment(cursor.insertAt, 'base')], 0);
    }
    if (next is RadicalNode) {
      return Cursor(
          [...cursor.path, CursorSegment(cursor.insertAt, 'radicand')], 0);
    }
    if (next is ParenNode) {
      return Cursor(
          [...cursor.path, CursorSegment(cursor.insertAt, 'children')], 0);
    }
    return Cursor(cursor.path, cursor.insertAt + 1);
  }
  if (cursor.path.isEmpty) return cursor;
  final parentSeg = cursor.path.last;
  final parentPath = cursor.path.sublist(0, cursor.path.length - 1);
  if (parentSeg.slot == 'numerator') {
    return Cursor(
        [...parentPath, CursorSegment(parentSeg.nodeIndex, 'denominator')], 0);
  }
  if (parentSeg.slot == 'base') {
    return Cursor(
        [...parentPath, CursorSegment(parentSeg.nodeIndex, 'exponent')], 0);
  }
  return Cursor(parentPath, parentSeg.nodeIndex + 1);
}

Cursor moveCursorLeft(List<ASTNode> root, Cursor cursor) {
  if (cursor.insertAt > 0) {
    final list = _getList(root, cursor.path);
    final prev = list[cursor.insertAt - 1];
    if (prev is FractionNode) {
      return Cursor(
          [...cursor.path, CursorSegment(cursor.insertAt - 1, 'denominator')],
          prev.denominator.length);
    }
    if (prev is ExponentNode) {
      return Cursor(
          [...cursor.path, CursorSegment(cursor.insertAt - 1, 'exponent')],
          prev.exponent.length);
    }
    if (prev is RadicalNode) {
      return Cursor(
          [...cursor.path, CursorSegment(cursor.insertAt - 1, 'radicand')],
          prev.radicand.length);
    }
    if (prev is ParenNode) {
      return Cursor(
          [...cursor.path, CursorSegment(cursor.insertAt - 1, 'children')],
          prev.children.length);
    }
    return Cursor(cursor.path, cursor.insertAt - 1);
  }
  if (cursor.path.isEmpty) return cursor;
  final parentSeg = cursor.path.last;
  final parentPath = cursor.path.sublist(0, cursor.path.length - 1);
  if (parentSeg.slot == 'denominator') {
    final frac = _parentList(root, cursor)[parentSeg.nodeIndex] as FractionNode;
    return Cursor(
        [...parentPath, CursorSegment(parentSeg.nodeIndex, 'numerator')],
        frac.numerator.length);
  }
  if (parentSeg.slot == 'exponent') {
    final exp = _parentList(root, cursor)[parentSeg.nodeIndex] as ExponentNode;
    return Cursor(
        [...parentPath, CursorSegment(parentSeg.nodeIndex, 'base')],
        exp.base.length);
  }
  return Cursor(parentPath, parentSeg.nodeIndex);
}

Cursor moveCursorUp(List<ASTNode> root, Cursor cursor) {
  if (cursor.path.isEmpty) return cursor;
  final parentSeg = cursor.path.last;
  final parentPath = cursor.path.sublist(0, cursor.path.length - 1);
  if (parentSeg.slot == 'denominator') {
    final frac = _getList(root, parentPath)[parentSeg.nodeIndex] as FractionNode;
    return Cursor(
        [...parentPath, CursorSegment(parentSeg.nodeIndex, 'numerator')],
        min(cursor.insertAt, frac.numerator.length));
  }
  if (parentSeg.slot == 'exponent') {
    final exp = _getList(root, parentPath)[parentSeg.nodeIndex] as ExponentNode;
    return Cursor(
        [...parentPath, CursorSegment(parentSeg.nodeIndex, 'base')],
        min(cursor.insertAt, exp.base.length));
  }
  return cursor;
}

Cursor moveCursorDown(List<ASTNode> root, Cursor cursor) {
  if (cursor.path.isEmpty) return cursor;
  final parentSeg = cursor.path.last;
  final parentPath = cursor.path.sublist(0, cursor.path.length - 1);
  if (parentSeg.slot == 'numerator') {
    final frac = _getList(root, parentPath)[parentSeg.nodeIndex] as FractionNode;
    return Cursor(
        [...parentPath, CursorSegment(parentSeg.nodeIndex, 'denominator')],
        min(cursor.insertAt, frac.denominator.length));
  }
  if (parentSeg.slot == 'base') {
    final exp = _getList(root, parentPath)[parentSeg.nodeIndex] as ExponentNode;
    return Cursor(
        [...parentPath, CursorSegment(parentSeg.nodeIndex, 'exponent')],
        min(cursor.insertAt, exp.exponent.length));
  }
  return cursor;
}
