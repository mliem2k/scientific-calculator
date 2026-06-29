import 'package:flutter/material.dart';
import 'package:scientific_calculator/core/ast/types.dart';
import 'package:scientific_calculator/theme/calc_theme.dart';
import 'package:scientific_calculator/widgets/ast/cursor_widget.dart';

bool _pathEquals(List<CursorSegment> a, List<CursorSegment> b) {
  if (a.length != b.length) return false;
  for (int i = 0; i < a.length; i++) {
    if (a[i].nodeIndex != b[i].nodeIndex || a[i].slot != b[i].slot) {
      return false;
    }
  }
  return true;
}

class ASTRenderer extends StatelessWidget {
  final List<ASTNode> nodes;
  final Cursor cursor;
  final List<CursorSegment> path;
  final void Function(List<CursorSegment> path, int insertAt) onCursorJump;
  final GlobalKey? rowKey;

  const ASTRenderer({
    super.key,
    required this.nodes,
    required this.cursor,
    required this.path,
    required this.onCursorJump,
    this.rowKey,
  });

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;
    final children = <Widget>[];

    for (int i = 0; i <= nodes.length; i++) {
      if (_pathEquals(cursor.path, path) && cursor.insertAt == i) {
        children.add(const CursorWidget());
      }
      if (i < nodes.length) {
        children.add(_buildNode(context, ct, nodes[i], i));
      }
    }

    return Row(
      key: rowKey,
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: children,
    );
  }

  Widget _buildNode(
      BuildContext context, CalcTheme ct, ASTNode node, int nodeIndex) {
    switch (node) {
      case NumberNode n:
        return GestureDetector(
          onTapDown: (_) => onCursorJump(path, nodeIndex + 1),
          child: Text(n.value, style: TextStyle(color: ct.digitText)),
        );

      case OperatorNode n:
        return GestureDetector(
          onTapDown: (_) => onCursorJump(path, nodeIndex + 1),
          child: Text(n.op, style: TextStyle(color: ct.opText)),
        );

      case ConstantNode n:
        final style = TextStyle(color: ct.expressionText);
        late final Widget textWidget;
        if (n.name == 'pi') {
          textWidget =
              Text('π', style: style.copyWith(fontStyle: FontStyle.italic));
        } else if (n.name == 'e') {
          textWidget =
              Text('e', style: style.copyWith(fontStyle: FontStyle.italic));
        } else if (n.name == 'Ans') {
          textWidget = Text('Ans', style: style);
        } else {
          textWidget = Text(
            n.name,
            style: style.copyWith(fontWeight: FontWeight.bold),
          );
        }
        return GestureDetector(
          onTapDown: (_) => onCursorJump(path, nodeIndex + 1),
          child: textWidget,
        );

      case FractionNode n:
        return _FractionWidget(
          node: n,
          nodeIndex: nodeIndex,
          cursor: cursor,
          path: path,
          onCursorJump: onCursorJump,
          ct: ct,
        );

      case ExponentNode n:
        return _ExponentWidget(
          node: n,
          nodeIndex: nodeIndex,
          cursor: cursor,
          path: path,
          onCursorJump: onCursorJump,
        );

      case RadicalNode n:
        return _RadicalWidget(
          node: n,
          nodeIndex: nodeIndex,
          cursor: cursor,
          path: path,
          onCursorJump: onCursorJump,
          ct: ct,
        );

      case FunctionNode n:
        return _FunctionWidget(
          node: n,
          nodeIndex: nodeIndex,
          cursor: cursor,
          path: path,
          onCursorJump: onCursorJump,
          ct: ct,
        );

      case FactorialNode n:
        return _FactorialWidget(
          node: n,
          nodeIndex: nodeIndex,
          cursor: cursor,
          path: path,
          onCursorJump: onCursorJump,
          ct: ct,
        );

      case NcrNode n:
        return _NcrWidget(
          node: n,
          nodeIndex: nodeIndex,
          cursor: cursor,
          path: path,
          onCursorJump: onCursorJump,
          ct: ct,
        );

      case NprNode n:
        return _NprWidget(
          node: n,
          nodeIndex: nodeIndex,
          cursor: cursor,
          path: path,
          onCursorJump: onCursorJump,
          ct: ct,
        );

      case ParenNode n:
        return _ParenWidget(
          node: n,
          nodeIndex: nodeIndex,
          cursor: cursor,
          path: path,
          onCursorJump: onCursorJump,
          ct: ct,
        );
    }
  }
}

class _FractionWidget extends StatelessWidget {
  final FractionNode node;
  final int nodeIndex;
  final Cursor cursor;
  final List<CursorSegment> path;
  final void Function(List<CursorSegment>, int) onCursorJump;
  final CalcTheme ct;

  const _FractionWidget({
    required this.node,
    required this.nodeIndex,
    required this.cursor,
    required this.path,
    required this.onCursorJump,
    required this.ct,
  });

  Widget _slot(
    BuildContext context,
    TextStyle style,
    double childSize,
    List<ASTNode> nodes,
    List<CursorSegment> slotPath,
  ) {
    final isActive = _pathEquals(cursor.path, slotPath);
    final isEmpty = nodes.isEmpty && !isActive;

    Widget content = DefaultTextStyle(
      style: style,
      child: isEmpty
          ? Text(
              '□',
              style: TextStyle(
                color: ct.expressionText.withValues(alpha: 80 / 255),
                fontSize: childSize,
              ),
            )
          : ASTRenderer(
              nodes: nodes,
              cursor: cursor,
              path: slotPath,
              onCursorJump: onCursorJump,
            ),
    );

    if (isEmpty) {
      content = GestureDetector(
        onTapDown: (_) => onCursorJump(slotPath, 0),
        child: content,
      );
    }

    return ConstrainedBox(
      constraints: BoxConstraints(
        minWidth: childSize * 0.6,
        minHeight: childSize * 0.8,
      ),
      child: Center(child: content),
    );
  }

  @override
  Widget build(BuildContext context) {
    final currentStyle = DefaultTextStyle.of(context).style;
    final currentSize = currentStyle.fontSize ?? 16.0;
    final childSize = currentSize * 0.7;
    final childStyle = currentStyle.copyWith(fontSize: childSize);

    final numPath = [...path, CursorSegment(nodeIndex, 'numerator')];
    final denPath = [...path, CursorSegment(nodeIndex, 'denominator')];

    return IntrinsicWidth(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _slot(context, childStyle, childSize, node.numerator, numPath),
          Container(height: 2, color: ct.expressionText),
          _slot(context, childStyle, childSize, node.denominator, denPath),
        ],
      ),
    );
  }
}

class _ExponentWidget extends StatelessWidget {
  final ExponentNode node;
  final int nodeIndex;
  final Cursor cursor;
  final List<CursorSegment> path;
  final void Function(List<CursorSegment>, int) onCursorJump;

  const _ExponentWidget({
    required this.node,
    required this.nodeIndex,
    required this.cursor,
    required this.path,
    required this.onCursorJump,
  });

  @override
  Widget build(BuildContext context) {
    final currentStyle = DefaultTextStyle.of(context).style;
    final currentSize = currentStyle.fontSize ?? 16.0;
    final expSize = currentSize * 0.65;

    final basePath = [...path, CursorSegment(nodeIndex, 'base')];
    final expPath = [...path, CursorSegment(nodeIndex, 'exponent')];

    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ASTRenderer(
          nodes: node.base,
          cursor: cursor,
          path: basePath,
          onCursorJump: onCursorJump,
        ),
        Transform.translate(
          offset: Offset(0, -(currentSize * 0.4)),
          child: DefaultTextStyle(
            style: currentStyle.copyWith(fontSize: expSize),
            child: ASTRenderer(
              nodes: node.exponent,
              cursor: cursor,
              path: expPath,
              onCursorJump: onCursorJump,
            ),
          ),
        ),
      ],
    );
  }
}

class _RadicalWidget extends StatelessWidget {
  final RadicalNode node;
  final int nodeIndex;
  final Cursor cursor;
  final List<CursorSegment> path;
  final void Function(List<CursorSegment>, int) onCursorJump;
  final CalcTheme ct;

  const _RadicalWidget({
    required this.node,
    required this.nodeIndex,
    required this.cursor,
    required this.path,
    required this.onCursorJump,
    required this.ct,
  });

  @override
  Widget build(BuildContext context) {
    final currentStyle = DefaultTextStyle.of(context).style;
    final currentSize = currentStyle.fontSize ?? 16.0;
    final degreeSize = currentSize * 0.5;

    final degreePath = [...path, CursorSegment(nodeIndex, 'degree')];
    final radicandPath = [...path, CursorSegment(nodeIndex, 'radicand')];

    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        if (node.degree.isNotEmpty)
          Transform.translate(
            offset: const Offset(0, -4),
            child: DefaultTextStyle(
              style: currentStyle.copyWith(fontSize: degreeSize),
              child: ASTRenderer(
                nodes: node.degree,
                cursor: cursor,
                path: degreePath,
                onCursorJump: onCursorJump,
              ),
            ),
          ),
        Text('√(', style: TextStyle(color: ct.expressionText)),
        ASTRenderer(
          nodes: node.radicand,
          cursor: cursor,
          path: radicandPath,
          onCursorJump: onCursorJump,
        ),
        Text(')', style: TextStyle(color: ct.expressionText)),
      ],
    );
  }
}

class _FunctionWidget extends StatelessWidget {
  final FunctionNode node;
  final int nodeIndex;
  final Cursor cursor;
  final List<CursorSegment> path;
  final void Function(List<CursorSegment>, int) onCursorJump;
  final CalcTheme ct;

  const _FunctionWidget({
    required this.node,
    required this.nodeIndex,
    required this.cursor,
    required this.path,
    required this.onCursorJump,
    required this.ct,
  });

  @override
  Widget build(BuildContext context) {
    final currentStyle = DefaultTextStyle.of(context).style;
    final currentSize = currentStyle.fontSize ?? 16.0;
    final expSize = currentSize * 0.65;

    final argPath = [...path, CursorSegment(nodeIndex, 'argument')];

    if (node.name == FunctionName.pow10) {
      final expPath = [...path, CursorSegment(nodeIndex, 'argument')];
      return Row(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('10', style: TextStyle(color: ct.expressionText)),
          Transform.translate(
            offset: Offset(0, -(currentSize * 0.4)),
            child: DefaultTextStyle(
              style: currentStyle.copyWith(fontSize: expSize),
              child: ASTRenderer(
                nodes: node.argument,
                cursor: cursor,
                path: expPath,
                onCursorJump: onCursorJump,
              ),
            ),
          ),
        ],
      );
    }

    if (node.name == FunctionName.exp) {
      final expPath = [...path, CursorSegment(nodeIndex, 'argument')];
      return Row(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'e',
            style: TextStyle(
              color: ct.expressionText,
              fontStyle: FontStyle.italic,
            ),
          ),
          Transform.translate(
            offset: Offset(0, -(currentSize * 0.4)),
            child: DefaultTextStyle(
              style: currentStyle.copyWith(fontSize: expSize),
              child: ASTRenderer(
                nodes: node.argument,
                cursor: cursor,
                path: expPath,
                onCursorJump: onCursorJump,
              ),
            ),
          ),
        ],
      );
    }

    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Text(
          '${node.name.name}(',
          style: TextStyle(color: ct.expressionText),
        ),
        ASTRenderer(
          nodes: node.argument,
          cursor: cursor,
          path: argPath,
          onCursorJump: onCursorJump,
        ),
        Text(')', style: TextStyle(color: ct.expressionText)),
      ],
    );
  }
}

class _FactorialWidget extends StatelessWidget {
  final FactorialNode node;
  final int nodeIndex;
  final Cursor cursor;
  final List<CursorSegment> path;
  final void Function(List<CursorSegment>, int) onCursorJump;
  final CalcTheme ct;

  const _FactorialWidget({
    required this.node,
    required this.nodeIndex,
    required this.cursor,
    required this.path,
    required this.onCursorJump,
    required this.ct,
  });

  @override
  Widget build(BuildContext context) {
    final operandPath = [...path, CursorSegment(nodeIndex, 'operand')];

    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        ASTRenderer(
          nodes: node.operand,
          cursor: cursor,
          path: operandPath,
          onCursorJump: onCursorJump,
        ),
        Text('!', style: TextStyle(color: ct.expressionText)),
      ],
    );
  }
}

class _NcrWidget extends StatelessWidget {
  final NcrNode node;
  final int nodeIndex;
  final Cursor cursor;
  final List<CursorSegment> path;
  final void Function(List<CursorSegment>, int) onCursorJump;
  final CalcTheme ct;

  const _NcrWidget({
    required this.node,
    required this.nodeIndex,
    required this.cursor,
    required this.path,
    required this.onCursorJump,
    required this.ct,
  });

  @override
  Widget build(BuildContext context) {
    final currentStyle = DefaultTextStyle.of(context).style;
    final currentSize = currentStyle.fontSize ?? 16.0;
    final childSize = currentSize * 0.7;

    final nPath = [...path, CursorSegment(nodeIndex, 'n')];
    final rPath = [...path, CursorSegment(nodeIndex, 'r')];

    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        DefaultTextStyle(
          style: currentStyle.copyWith(fontSize: childSize),
          child: ASTRenderer(
            nodes: node.n,
            cursor: cursor,
            path: nPath,
            onCursorJump: onCursorJump,
          ),
        ),
        Text('C', style: TextStyle(color: ct.expressionText)),
        DefaultTextStyle(
          style: currentStyle.copyWith(fontSize: childSize),
          child: ASTRenderer(
            nodes: node.r,
            cursor: cursor,
            path: rPath,
            onCursorJump: onCursorJump,
          ),
        ),
      ],
    );
  }
}

class _NprWidget extends StatelessWidget {
  final NprNode node;
  final int nodeIndex;
  final Cursor cursor;
  final List<CursorSegment> path;
  final void Function(List<CursorSegment>, int) onCursorJump;
  final CalcTheme ct;

  const _NprWidget({
    required this.node,
    required this.nodeIndex,
    required this.cursor,
    required this.path,
    required this.onCursorJump,
    required this.ct,
  });

  @override
  Widget build(BuildContext context) {
    final currentStyle = DefaultTextStyle.of(context).style;
    final currentSize = currentStyle.fontSize ?? 16.0;
    final childSize = currentSize * 0.7;

    final nPath = [...path, CursorSegment(nodeIndex, 'n')];
    final rPath = [...path, CursorSegment(nodeIndex, 'r')];

    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        DefaultTextStyle(
          style: currentStyle.copyWith(fontSize: childSize),
          child: ASTRenderer(
            nodes: node.n,
            cursor: cursor,
            path: nPath,
            onCursorJump: onCursorJump,
          ),
        ),
        Text('P', style: TextStyle(color: ct.expressionText)),
        DefaultTextStyle(
          style: currentStyle.copyWith(fontSize: childSize),
          child: ASTRenderer(
            nodes: node.r,
            cursor: cursor,
            path: rPath,
            onCursorJump: onCursorJump,
          ),
        ),
      ],
    );
  }
}

class _ParenWidget extends StatelessWidget {
  final ParenNode node;
  final int nodeIndex;
  final Cursor cursor;
  final List<CursorSegment> path;
  final void Function(List<CursorSegment>, int) onCursorJump;
  final CalcTheme ct;

  const _ParenWidget({
    required this.node,
    required this.nodeIndex,
    required this.cursor,
    required this.path,
    required this.onCursorJump,
    required this.ct,
  });

  @override
  Widget build(BuildContext context) {
    final childrenPath = [...path, CursorSegment(nodeIndex, 'children')];

    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Text('(', style: TextStyle(color: ct.expressionText)),
        ASTRenderer(
          nodes: node.children,
          cursor: cursor,
          path: childrenPath,
          onCursorJump: onCursorJump,
        ),
        Text(')', style: TextStyle(color: ct.expressionText)),
      ],
    );
  }
}
