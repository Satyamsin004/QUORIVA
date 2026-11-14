import React, { useState, useEffect, useRef, useMemo } from 'react';
import { ZoomIn, ZoomOut, RotateCcw, Layers } from 'lucide-react';
import { GraphNode, GraphEdge, TopicDifficulty } from '@/types';

interface KnowledgeGraphCanvasProps {
  nodes: GraphNode[];
  edges: GraphEdge[];
  onSelectTopic: (topicSlug: string) => void;
  onExpandTopic?: (topicSlug: string) => void;
  selectedTopicSlug?: string | null;
  className?: string;
}

interface LayoutNode extends GraphNode {
  x: number;
  y: number;
  vx: number;
  vy: number;
}

const DIFFICULTY_COLORS: Record<TopicDifficulty, { bg: string; border: string; text: string; fill: string }> = {
  BEGINNER: {
    bg: 'bg-emerald-950/80',
    border: 'border-emerald-500/60',
    text: 'text-emerald-300',
    fill: '#10b981',
  },
  INTERMEDIATE: {
    bg: 'bg-blue-950/80',
    border: 'border-blue-500/60',
    text: 'text-blue-300',
    fill: '#3b82f6',
  },
  ADVANCED: {
    bg: 'bg-purple-950/80',
    border: 'border-purple-500/60',
    text: 'text-purple-300',
    fill: '#a855f7',
  },
};

export const KnowledgeGraphCanvas: React.FC<KnowledgeGraphCanvasProps> = ({
  nodes,
  edges,
  onSelectTopic,
  onExpandTopic,
  selectedTopicSlug,
  className = '',
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState({ width: 900, height: 600 });
  const [transform, setTransform] = useState({ x: 0, y: 0, scale: 1 });
  const [isPanning, setIsPanning] = useState(false);
  const [panStart, setPanStart] = useState({ x: 0, y: 0 });
  const [hoveredNode, setHoveredNode] = useState<LayoutNode | null>(null);
  const [draggedNodeId, setDraggedNodeId] = useState<string | null>(null);

  // Measure container dimensions
  useEffect(() => {
    if (!containerRef.current) return;
    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        if (width > 0 && height > 0) {
          setDimensions({ width, height });
        }
      }
    });
    observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, []);

  // Compute force layout
  const layoutNodes = useMemo(() => {
    if (!nodes || nodes.length === 0) return [];

    const { width, height } = dimensions;
    const cx = width / 2;
    const cy = height / 2;

    // Find center node if any
    const centerNode = nodes.find((n) => n.isCenter || n.center);

    // Initial position in a circular / spiral layout
    const initialNodes: LayoutNode[] = nodes.map((node, i) => {
      if (node.id === centerNode?.id) {
        return { ...node, x: cx, y: cy, vx: 0, vy: 0 };
      }
      const angle = (2 * Math.PI * i) / Math.max(1, nodes.length - 1);
      const radius = Math.min(width, height) * 0.35 + (i % 3) * 30;
      return {
        ...node,
        x: cx + radius * Math.cos(angle),
        y: cy + radius * Math.sin(angle),
        vx: 0,
        vy: 0,
      };
    });

    const nodeMap = new Map<string, LayoutNode>();
    initialNodes.forEach((n) => nodeMap.set(n.id, n));

    // Fast simulation relaxation (40 iterations)
    const iterations = 40;
    const k = Math.sqrt((width * height) / Math.max(1, nodes.length)) * 0.8;

    for (let iter = 0; iter < iterations; iter++) {
      const temperature = 1 - iter / iterations;

      // Repulsion between all pairs
      for (let i = 0; i < initialNodes.length; i++) {
        for (let j = i + 1; j < initialNodes.length; j++) {
          const n1 = initialNodes[i];
          const n2 = initialNodes[j];
          const dx = n2.x - n1.x;
          const dy = n2.y - n1.y;
          const dist = Math.max(20, Math.sqrt(dx * dx + dy * dy));
          const force = (k * k) / dist;
          const fx = (dx / dist) * force * 0.05 * temperature;
          const fy = (dy / dist) * force * 0.05 * temperature;

          if (n1.id !== centerNode?.id) {
            n1.x -= fx;
            n1.y -= fy;
          }
          if (n2.id !== centerNode?.id) {
            n2.x += fx;
            n2.y += fy;
          }
        }
      }

      // Attraction along edges
      edges.forEach((edge) => {
        const source = nodeMap.get(edge.source);
        const target = nodeMap.get(edge.target);
        if (source && target) {
          const dx = target.x - source.x;
          const dy = target.y - source.y;
          const dist = Math.max(20, Math.sqrt(dx * dx + dy * dy));
          const force = (dist * dist) / k;
          const fx = (dx / dist) * force * 0.03 * temperature;
          const fy = (dy / dist) * force * 0.03 * temperature;

          if (source.id !== centerNode?.id) {
            source.x += fx;
            source.y += fy;
          }
          if (target.id !== centerNode?.id) {
            target.x -= fx;
            target.y -= fy;
          }
        }
      });

      // Gravity towards center
      initialNodes.forEach((node) => {
        if (node.id === centerNode?.id) return;
        const dx = cx - node.x;
        const dy = cy - node.y;
        node.x += dx * 0.01 * temperature;
        node.y += dy * 0.01 * temperature;
      });
    }

    return initialNodes;
  }, [nodes, edges, dimensions]);

  const nodeMap = useMemo(() => {
    const map = new Map<string, LayoutNode>();
    layoutNodes.forEach((n) => map.set(n.id, n));
    return map;
  }, [layoutNodes]);

  // Pan and Zoom handlers
  const handleMouseDown = (e: React.MouseEvent) => {
    if (e.target instanceof SVGElement && e.target.tagName !== 'svg') {
      return; // click on node or edge
    }
    setIsPanning(true);
    setPanStart({ x: e.clientX - transform.x, y: e.clientY - transform.y });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (isPanning) {
      setTransform((prev) => ({
        ...prev,
        x: e.clientX - panStart.x,
        y: e.clientY - panStart.y,
      }));
    } else if (draggedNodeId) {
      const dragged = nodeMap.get(draggedNodeId);
      if (dragged) {
        dragged.x += e.movementX / transform.scale;
        dragged.y += e.movementY / transform.scale;
        setHoveredNode({ ...dragged });
      }
    }
  };

  const handleMouseUp = () => {
    setIsPanning(false);
    setDraggedNodeId(null);
  };

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const zoomFactor = e.deltaY < 0 ? 1.1 : 0.9;
    const newScale = Math.min(Math.max(transform.scale * zoomFactor, 0.4), 2.5);
    setTransform((prev) => ({ ...prev, scale: newScale }));
  };

  const handleZoomIn = () => {
    setTransform((prev) => ({ ...prev, scale: Math.min(prev.scale * 1.2, 2.5) }));
  };

  const handleZoomOut = () => {
    setTransform((prev) => ({ ...prev, scale: Math.max(prev.scale / 1.2, 0.4) }));
  };

  const handleReset = () => {
    setTransform({ x: 0, y: 0, scale: 1 });
  };

  return (
    <div
      ref={containerRef}
      className={`relative w-full h-[600px] bg-slate-950/80 rounded-xl border border-slate-800 overflow-hidden select-none ${className}`}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
      onWheel={handleWheel}
    >
      {/* Background Grid Pattern */}
      <div
        className="absolute inset-0 pointer-events-none opacity-20"
        style={{
          backgroundImage: 'radial-gradient(circle, #475569 1px, transparent 1px)',
          backgroundSize: '24px 24px',
        }}
      />

      {/* Canvas Controls */}
      <div className="absolute top-4 right-4 z-10 flex items-center gap-1.5 bg-slate-900/90 border border-slate-700/80 backdrop-blur rounded-lg p-1.5 shadow-lg">
        <button
          onClick={handleZoomIn}
          title="Zoom In"
          className="p-1.5 text-slate-300 hover:text-white hover:bg-slate-800 rounded transition"
        >
          <ZoomIn className="w-4 h-4" />
        </button>
        <button
          onClick={handleZoomOut}
          title="Zoom Out"
          className="p-1.5 text-slate-300 hover:text-white hover:bg-slate-800 rounded transition"
        >
          <ZoomOut className="w-4 h-4" />
        </button>
        <button
          onClick={handleReset}
          title="Reset View"
          className="p-1.5 text-slate-300 hover:text-white hover:bg-slate-800 rounded transition"
        >
          <RotateCcw className="w-4 h-4" />
        </button>
      </div>

      {/* Legend */}
      <div className="absolute bottom-4 left-4 z-10 bg-slate-900/85 border border-slate-800 backdrop-blur rounded-lg p-3 text-xs text-slate-300 shadow-md">
        <div className="font-semibold text-slate-200 mb-2 flex items-center gap-1.5">
          <Layers className="w-3.5 h-3.5 text-primary-400" /> Topic Difficulty
        </div>
        <div className="flex flex-col gap-1.5">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-emerald-500" />
            <span>Beginner (Foundation)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-blue-500" />
            <span>Intermediate (Core Concept)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-purple-500" />
            <span>Advanced (Specialization)</span>
          </div>
        </div>
        <div className="mt-2.5 pt-2 border-t border-slate-800 text-[11px] text-slate-400">
          <span className="text-amber-400 font-medium">Solid Arrow:</span> Prerequisite &rarr;
          <br />
          <span className="text-slate-400 font-medium">Dashed Line:</span> Related Concept
        </div>
      </div>

      {/* SVG Canvas */}
      <svg
        className="w-full h-full cursor-grab active:cursor-grabbing"
        style={{
          transform: `translate(${transform.x}px, ${transform.y}px) scale(${transform.scale})`,
          transformOrigin: '0 0',
        }}
      >
        <defs>
          {/* Arrow marker for PREREQUISITE_OF */}
          <marker
            id="arrow-prereq"
            viewBox="0 0 10 10"
            refX="22"
            refY="5"
            markerWidth="6"
            markerHeight="6"
            orient="auto-start-reverse"
          >
            <path d="M 0 1 L 10 5 L 0 9 z" fill="#38bdf8" />
          </marker>
          {/* Arrow marker for other relationships */}
          <marker
            id="arrow-related"
            viewBox="0 0 10 10"
            refX="22"
            refY="5"
            markerWidth="5"
            markerHeight="5"
            orient="auto-start-reverse"
          >
            <path d="M 0 1 L 10 5 L 0 9 z" fill="#64748b" />
          </marker>
        </defs>

        {/* Directed Edges */}
        <g className="edges">
          {edges.map((edge) => {
            const source = nodeMap.get(edge.source);
            const target = nodeMap.get(edge.target);
            if (!source || !target) return null;

            const isPrereq = edge.relationshipType === 'PREREQUISITE_OF';
            const isHighlighted =
              hoveredNode && (hoveredNode.id === source.id || hoveredNode.id === target.id);

            return (
              <g key={edge.id}>
                <line
                  x1={source.x}
                  y1={source.y}
                  x2={target.x}
                  y2={target.y}
                  stroke={
                    isHighlighted
                      ? '#38bdf8'
                      : isPrereq
                      ? '#3b82f6'
                      : '#475569'
                  }
                  strokeWidth={isHighlighted ? 2.5 : isPrereq ? 1.8 : 1.2}
                  strokeDasharray={isPrereq ? undefined : '4 4'}
                  strokeOpacity={isHighlighted ? 0.9 : 0.4}
                  markerEnd={isPrereq ? 'url(#arrow-prereq)' : 'url(#arrow-related)'}
                />
              </g>
            );
          })}
        </g>

        {/* Nodes */}
        <g className="nodes">
          {layoutNodes.map((node) => {
            const isSelected = selectedTopicSlug === node.slug;
            const isCenter = node.isCenter || node.center;
            const style = DIFFICULTY_COLORS[node.difficultyLevel] || DIFFICULTY_COLORS.BEGINNER;
            const radius = isCenter ? 32 : isSelected ? 28 : 22;

            return (
              <g
                key={node.id}
                transform={`translate(${node.x}, ${node.y})`}
                className="cursor-pointer group"
                onClick={(e) => {
                  e.stopPropagation();
                  onSelectTopic(node.slug);
                }}
                onMouseEnter={() => setHoveredNode(node)}
                onMouseLeave={() => setHoveredNode(null)}
                onDoubleClick={(e) => {
                  e.stopPropagation();
                  if (onExpandTopic) onExpandTopic(node.slug);
                }}
              >
                {/* Center / Selected Glow */}
                {(isCenter || isSelected) && (
                  <circle
                    r={radius + 8}
                    className="animate-pulse"
                    fill="none"
                    stroke={isCenter ? '#f59e0b' : '#38bdf8'}
                    strokeWidth="3"
                    strokeOpacity="0.6"
                  />
                )}

                {/* Node Outer Circle */}
                <circle
                  r={radius}
                  fill={style.fill}
                  fillOpacity={isCenter ? 0.35 : 0.2}
                  stroke={isCenter ? '#fbbf24' : style.fill}
                  strokeWidth={isCenter ? 3 : isSelected ? 2.5 : 1.5}
                  className="transition-all duration-200 group-hover:scale-110"
                />

                {/* Node Core Icon / Circle */}
                <circle r={radius * 0.4} fill={style.fill} fillOpacity={0.8} />

                {/* Node Label */}
                <text
                  y={radius + 15}
                  textAnchor="middle"
                  className={`text-[11px] font-medium pointer-events-none drop-shadow-md fill-slate-200 transition ${
                    isSelected ? 'font-bold fill-primary-400' : ''
                  }`}
                >
                  {node.name.length > 20 ? `${node.name.slice(0, 18)}…` : node.name}
                </text>
              </g>
            );
          })}
        </g>
      </svg>

      {/* Hover Tooltip */}
      {hoveredNode && (
        <div
          className="absolute pointer-events-none z-20 bg-slate-900/95 border border-slate-700 backdrop-blur rounded-lg px-3 py-2 text-xs shadow-xl max-w-xs"
          style={{
            left: `${hoveredNode.x * transform.scale + transform.x + 20}px`,
            top: `${hoveredNode.y * transform.scale + transform.y - 20}px`,
          }}
        >
          <div className="font-semibold text-white">{hoveredNode.name}</div>
          <div className="text-[11px] text-slate-400 mt-0.5">
            Subject: {hoveredNode.subjectName}
          </div>
          <div className="flex items-center gap-2 mt-1.5 text-[10px]">
            <span
              className={`px-1.5 py-0.5 rounded font-medium ${
                hoveredNode.difficultyLevel === 'BEGINNER'
                  ? 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                  : hoveredNode.difficultyLevel === 'INTERMEDIATE'
                  ? 'bg-blue-950 text-blue-300 border border-blue-800'
                  : 'bg-purple-950 text-purple-300 border border-purple-800'
              }`}
            >
              {hoveredNode.difficultyLevel}
            </span>
            <span className="text-slate-400">
              Prereqs: {hoveredNode.prerequisiteCount} | Unlocks: {hoveredNode.unlockedCount}
            </span>
          </div>
          <div className="text-[10px] text-primary-400 mt-1 italic">
            Click to view details · Double-click to expand graph
          </div>
        </div>
      )}
    </div>
  );
};
