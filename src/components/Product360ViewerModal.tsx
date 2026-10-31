import React, { useEffect, useRef, useState, useCallback } from 'react';
import * as THREE from 'three';
import {
  X,
  RotateCcw,
  Play,
  Pause,
  ZoomIn,
  ZoomOut,
  Sparkles,
  ShoppingCart,
  Zap,
  Info,
  ChevronRight,
  Maximize2,
  CheckCircle2,
  Layers,
  Compass,
} from 'lucide-react';
import { Product } from './ProductDetailModal';
import { haptics } from '../utils/haptics';

interface Hotspot {
  id: string;
  title: string;
  description: string;
  angle: number; // in degrees (0 - 360)
  targetPosition: [number, number, number];
}

interface Product360ViewerModalProps {
  product: Product | null;
  isOpen: boolean;
  onClose: () => void;
  onAddToCart?: (product: Product) => void;
  onBuyNow?: (product: Product) => void;
}

export const Product360ViewerModal: React.FC<Product360ViewerModalProps> = ({
  product,
  isOpen,
  onClose,
  onAddToCart,
  onBuyNow,
}) => {
  const mountRef = useRef<HTMLDivElement>(null);
  const [rotationAngle, setRotationAngle] = useState(0); // 0 to 360 degrees
  const [isAutoRotating, setIsAutoRotating] = useState(true);
  const [activeHotspot, setActiveHotspot] = useState<Hotspot | null>(null);
  const [hasInteracted, setHasInteracted] = useState(false);
  const [viewMode, setViewMode] = useState<'3d' | 'frames'>('3d');
  const [zoomLevel, setZoomLevel] = useState(1);

  // References for Three.js state
  const sceneRef = useRef<THREE.Scene | null>(null);
  const rendererRef = useRef<THREE.WebGLRenderer | null>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const productGroupRef = useRef<THREE.Group | null>(null);
  const frameIdRef = useRef<number | null>(null);

  // Interaction dragging state
  const isDraggingRef = useRef(false);
  const previousPointerXRef = useRef(0);
  const velocityRef = useRef(0);

  // Category-specific hotspots
  const getHotspots = useCallback((prod: Product): Hotspot[] => {
    const cat = prod.category?.toLowerCase() || '';
    if (cat.includes('mobile') || cat.includes('phone')) {
      return [
        {
          id: 'display',
          title: '6.7″ 120Hz Fluid AMOLED Display',
          description: 'HDR10+ certified ultra-bright punch-hole screen with 240Hz touch sampling rate and SGS Eye Care certification.',
          angle: 0,
          targetPosition: [0, 0, 0.4],
        },
        {
          id: 'frame',
          title: '7.8mm Aerodynamic Metal Frame',
          description: 'Precision CNC engineered alloy chassis with antenna bands and tactile volume/power rockers.',
          angle: 90,
          targetPosition: [0.9, 0, 0],
        },
        {
          id: 'camera',
          title: '50MP Sony LYT OIS Dual Camera',
          description: 'Flagship optical image stabilization lens with sapphire glass cover, LED flash, and 4K Ultra-Steady video.',
          angle: 180,
          targetPosition: [0, 0.4, -0.4],
        },
        {
          id: 'port',
          title: '100W SuperVOOC Fast Charge Port',
          description: 'USB Type-C port with reverse charging support, stereo dual speakers, and noise-cancelling mic.',
          angle: 270,
          targetPosition: [-0.9, -0.7, 0],
        },
      ];
    } else if (cat.includes('electronics') || cat.includes('headphone') || cat.includes('audio')) {
      return [
        {
          id: 'driver',
          title: '40mm High-Res Neodymium Drivers',
          description: 'Deep dynamic punchy bass, crystal-clear vocal separation, and 360 Reality Audio surround tuning.',
          angle: 0,
          targetPosition: [0, 0, 0.5],
        },
        {
          id: 'cushion',
          title: 'Pressure-Relieving Memory Foam',
          description: 'Ultra-soft protein leather ear cushions designed for fatigue-free all-day music playback.',
          angle: 90,
          targetPosition: [0.8, 0, 0],
        },
        {
          id: 'battery',
          title: '50-Hour Extended Battery Life',
          description: 'Quick charge provides 3 hours playback on a 3-minute charge with integrated Type-C charging.',
          angle: 180,
          targetPosition: [0, 0, -0.5],
        },
        {
          id: 'mic',
          title: 'Multipoint Bluetooth 5.3 & Mic',
          description: 'Seamless switching between laptop and mobile with dual-beamforming microphones.',
          angle: 270,
          targetPosition: [-0.8, -0.3, 0],
        },
      ];
    } else if (cat.includes('home') || cat.includes('chair') || cat.includes('furniture')) {
      return [
        {
          id: 'backrest',
          title: 'Ergonomic S-Curve Lumbar Back',
          description: 'Adaptive breathable mesh with spine support relieving pressure during extended study/work hours.',
          angle: 0,
          targetPosition: [0, 0.4, 0.4],
        },
        {
          id: 'armrest',
          title: '3D Adjustable Soft PU Armrests',
          description: 'Multi-directional height and angle adjustment to support elbows and shoulders effortlessly.',
          angle: 90,
          targetPosition: [0.8, 0, 0],
        },
        {
          id: 'mechanism',
          title: '135° Synchro-Tilt Locking System',
          description: 'Class 4 gas lift cylinder certified for up to 140kg dynamic weight with pneumatic recline.',
          angle: 180,
          targetPosition: [0, 0, -0.5],
        },
        {
          id: 'wheel',
          title: 'Smooth 60mm Silent PU Casters',
          description: 'Anti-scratch dual-wheel casters providing 360° frictionless gliding across tile or hardwood.',
          angle: 270,
          targetPosition: [-0.7, -0.8, 0],
        },
      ];
    } else {
      return [
        {
          id: 'finish',
          title: 'Premium Brand Craftsmanship',
          description: '100% genuine authenticated materials certified with quality checks and Flipkart/AARVO standards.',
          angle: 0,
          targetPosition: [0, 0, 0.5],
        },
        {
          id: 'profile',
          title: 'Ergonomic Contoured Profile',
          description: 'Optimized proportions ensuring highest comfort, durability, and user satisfaction.',
          angle: 90,
          targetPosition: [0.7, 0, 0],
        },
        {
          id: 'back',
          title: 'Reinforced Back Structure',
          description: 'Built for longevity and verified through rigorous stress and drop-testing procedures.',
          angle: 180,
          targetPosition: [0, 0, -0.5],
        },
        {
          id: 'seal',
          title: 'Official Manufacturer Warranty',
          description: 'Includes full brand warranty, GST invoice, and 7-day hassle-free replacement coverage.',
          angle: 270,
          targetPosition: [-0.7, 0, 0],
        },
      ];
    }
  }, []);

  const hotspots = product ? getHotspots(product) : [];

  // Helper to build realistic 3D procedural product models
  const buildProductModel = useCallback((prod: Product): THREE.Group => {
    const group = new THREE.Group();
    const cat = prod.category?.toLowerCase() || '';

    // Studio base shadow disc
    const shadowGeo = new THREE.PlaneGeometry(3, 3);
    const canvas = document.createElement('canvas');
    canvas.width = 128;
    canvas.height = 128;
    const ctx = canvas.getContext('2d')!;
    const grad = ctx.createRadialGradient(64, 64, 10, 64, 64, 64);
    grad.addColorStop(0, 'rgba(10, 15, 30, 0.55)');
    grad.addColorStop(0.5, 'rgba(10, 15, 30, 0.25)');
    grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, 128, 128);
    const shadowTex = new THREE.CanvasTexture(canvas);
    const shadowMat = new THREE.MeshBasicMaterial({
      map: shadowTex,
      transparent: true,
      depthWrite: false,
    });
    const shadowMesh = new THREE.Mesh(shadowGeo, shadowMat);
    shadowMesh.rotation.x = -Math.PI / 2;
    shadowMesh.position.y = -1.1;
    group.add(shadowMesh);

    if (cat.includes('mobile') || cat.includes('phone')) {
      // 1. Phone Body Chassis (Rounded Box)
      const bodyGeo = new THREE.BoxGeometry(1.2, 2.3, 0.14);
      const bodyMat = new THREE.MeshStandardMaterial({
        color: 0x111625,
        metalness: 0.85,
        roughness: 0.25,
      });
      const bodyMesh = new THREE.Mesh(bodyGeo, bodyMat);
      group.add(bodyMesh);

      // 2. Metallic Bezel / Frame
      const frameGeo = new THREE.BoxGeometry(1.23, 2.33, 0.12);
      const frameMat = new THREE.MeshStandardMaterial({
        color: 0x38bdf8,
        metalness: 0.95,
        roughness: 0.15,
      });
      const frameMesh = new THREE.Mesh(frameGeo, frameMat);
      group.add(frameMesh);

      // 3. Front Display Glass with colorful screen
      const screenGeo = new THREE.PlaneGeometry(1.14, 2.22);
      const screenCanvas = document.createElement('canvas');
      screenCanvas.width = 256;
      screenCanvas.height = 512;
      const sCtx = screenCanvas.getContext('2d')!;
      const sGrad = sCtx.createLinearGradient(0, 0, 256, 512);
      sGrad.addColorStop(0, '#1e1b4b');
      sGrad.addColorStop(0.4, '#4338ca');
      sGrad.addColorStop(0.8, '#06b6d4');
      sGrad.addColorStop(1, '#10b981');
      sCtx.fillStyle = sGrad;
      sCtx.fillRect(0, 0, 256, 512);

      // Add clock and icons to simulate active smartphone
      sCtx.fillStyle = '#ffffff';
      sCtx.font = 'bold 36px sans-serif';
      sCtx.fillText('10:45', 75, 120);
      sCtx.font = '14px sans-serif';
      sCtx.fillText('Thursday, Oct 31', 70, 150);
      sCtx.font = 'bold 16px sans-serif';
      sCtx.fillText('AARVO 5G • 100%', 65, 50);

      const screenTex = new THREE.CanvasTexture(screenCanvas);
      const screenMat = new THREE.MeshStandardMaterial({
        map: screenTex,
        roughness: 0.1,
        metalness: 0.2,
      });
      const screenMesh = new THREE.Mesh(screenGeo, screenMat);
      screenMesh.position.z = 0.075;
      group.add(screenMesh);

      // 4. Rear Camera Module Bump
      const bumpGeo = new THREE.BoxGeometry(0.45, 0.75, 0.05);
      const bumpMat = new THREE.MeshStandardMaterial({
        color: 0x0f172a,
        metalness: 0.9,
        roughness: 0.2,
      });
      const bumpMesh = new THREE.Mesh(bumpGeo, bumpMat);
      bumpMesh.position.set(-0.3, 0.65, -0.09);
      group.add(bumpMesh);

      // 3 Camera Lenses with sapphire glass rings
      [-0.18, 0, 0.18].forEach((offsetY, i) => {
        const lensGeo = new THREE.CylinderGeometry(0.1, 0.1, 0.04, 24);
        const lensMat = new THREE.MeshStandardMaterial({
          color: 0x0284c7,
          metalness: 0.95,
          roughness: 0.05,
        });
        const lensMesh = new THREE.Mesh(lensGeo, lensMat);
        lensMesh.rotation.x = Math.PI / 2;
        lensMesh.position.set(-0.3, 0.65 + offsetY, -0.12);
        group.add(lensMesh);
      });
    } else if (cat.includes('electronics') || cat.includes('audio') || cat.includes('headphone')) {
      // Headphones 3D Model
      // Headband Arc
      const bandGeo = new THREE.TorusGeometry(1.1, 0.08, 16, 48, Math.PI);
      const bandMat = new THREE.MeshStandardMaterial({
        color: 0x1e293b,
        metalness: 0.7,
        roughness: 0.3,
      });
      const bandMesh = new THREE.Mesh(bandGeo, bandMat);
      bandMesh.rotation.z = Math.PI;
      bandMesh.position.y = 0.3;
      group.add(bandMesh);

      // Left & Right Earcups
      [-1.1, 1.1].forEach((posX) => {
        const cupGeo = new THREE.CylinderGeometry(0.45, 0.45, 0.3, 32);
        const cupMat = new THREE.MeshStandardMaterial({
          color: 0x0f172a,
          metalness: 0.8,
          roughness: 0.2,
        });
        const cupMesh = new THREE.Mesh(cupGeo, cupMat);
        cupMesh.rotation.z = Math.PI / 2;
        cupMesh.position.set(posX, 0.3, 0);
        group.add(cupMesh);

        // Soft Foam Cushion
        const padGeo = new THREE.TorusGeometry(0.4, 0.12, 16, 32);
        const padMat = new THREE.MeshStandardMaterial({
          color: 0x334155,
          roughness: 0.9,
          metalness: 0.1,
        });
        const padMesh = new THREE.Mesh(padGeo, padMat);
        padMesh.rotation.y = Math.PI / 2;
        padMesh.position.set(posX > 0 ? posX - 0.16 : posX + 0.16, 0.3, 0);
        group.add(padMesh);
      });
    } else if (cat.includes('home') || cat.includes('chair')) {
      // Ergonomic Chair 3D Model
      // Seat Cushion
      const seatGeo = new THREE.BoxGeometry(1.4, 0.15, 1.3);
      const seatMat = new THREE.MeshStandardMaterial({ color: 0x1e293b, roughness: 0.6 });
      const seatMesh = new THREE.Mesh(seatGeo, seatMat);
      seatMesh.position.y = -0.2;
      group.add(seatMesh);

      // Curved Backrest
      const backGeo = new THREE.BoxGeometry(1.3, 1.4, 0.12);
      const backMat = new THREE.MeshStandardMaterial({ color: 0x0284c7, roughness: 0.4, metalness: 0.2 });
      const backMesh = new THREE.Mesh(backGeo, backMat);
      backMesh.position.set(0, 0.5, -0.6);
      group.add(backMesh);

      // Gas lift column
      const stemGeo = new THREE.CylinderGeometry(0.1, 0.1, 0.7, 16);
      const stemMat = new THREE.MeshStandardMaterial({ color: 0x94a3b8, metalness: 0.9, roughness: 0.1 });
      const stemMesh = new THREE.Mesh(stemGeo, stemMat);
      stemMesh.position.y = -0.6;
      group.add(stemMesh);

      // 5-Star Wheeled Base
      const baseGeo = new THREE.CylinderGeometry(0.8, 0.8, 0.08, 5);
      const baseMat = new THREE.MeshStandardMaterial({ color: 0x0f172a, metalness: 0.8 });
      const baseMesh = new THREE.Mesh(baseGeo, baseMat);
      baseMesh.position.y = -0.95;
      group.add(baseMesh);
    } else {
      // Universal Premium Showcase Showcase Model (Rotatable Cylinder/Pillar display with emblem)
      const mainGeo = new THREE.CylinderGeometry(0.8, 0.8, 1.6, 32);
      const mainMat = new THREE.MeshStandardMaterial({
        color: 0x2418d8,
        metalness: 0.65,
        roughness: 0.25,
      });
      const mainMesh = new THREE.Mesh(mainGeo, mainMat);
      group.add(mainMesh);

      // Metallic Accent Rings
      [-0.8, 0, 0.8].forEach((posY) => {
        const ringGeo = new THREE.TorusGeometry(0.82, 0.03, 16, 48);
        const ringMat = new THREE.MeshStandardMaterial({
          color: 0xf59e0b,
          metalness: 0.95,
          roughness: 0.1,
        });
        const ringMesh = new THREE.Mesh(ringGeo, ringMat);
        ringMesh.rotation.x = Math.PI / 2;
        ringMesh.position.y = posY;
        group.add(ringMesh);
      });
    }

    return group;
  }, []);

  // Three.js Scene Setup & Render Loop
  useEffect(() => {
    if (!isOpen || !mountRef.current || !product) return;

    const container = mountRef.current;
    const width = container.clientWidth;
    const height = container.clientHeight;

    // Scene
    const scene = new THREE.Scene();
    sceneRef.current = scene;

    // Camera
    const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100);
    camera.position.set(0, 0.5, 4.2);
    cameraRef.current = camera;

    // Renderer
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'high-performance' });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.shadowMap.enabled = true;
    rendererRef.current = renderer;
    container.innerHTML = '';
    container.appendChild(renderer.domElement);

    // Studio Lighting Rig
    const ambientLight = new THREE.AmbientLight(0xffffff, 1.2);
    scene.add(ambientLight);

    const mainKeyLight = new THREE.DirectionalLight(0xffffff, 2.2);
    mainKeyLight.position.set(4, 6, 5);
    scene.add(mainKeyLight);

    const rimBlueLight = new THREE.DirectionalLight(0x60a5fa, 1.8);
    rimBlueLight.position.set(-5, 3, -4);
    scene.add(rimBlueLight);

    const warmAccentLight = new THREE.DirectionalLight(0xfef08a, 1.0);
    warmAccentLight.position.set(0, -3, 3);
    scene.add(warmAccentLight);

    // Build & Add Product Model
    const modelGroup = buildProductModel(product);
    scene.add(modelGroup);
    productGroupRef.current = modelGroup;

    // Animation Loop
    let lastTime = performance.now();
    const animate = () => {
      const now = performance.now();
      const delta = (now - lastTime) / 1000;
      lastTime = now;

      if (modelGroup) {
        // Inertia or Auto-Rotation
        if (!isDraggingRef.current) {
          if (Math.abs(velocityRef.current) > 0.001) {
            modelGroup.rotation.y += velocityRef.current;
            velocityRef.current *= 0.92; // Friction dampening
          } else if (isAutoRotating) {
            modelGroup.rotation.y += delta * 0.75; // Smooth 360 degree spin
          }
        }

        // Keep rotation normalized in 0 to 2*PI and update state angle
        const normalizedY = ((modelGroup.rotation.y % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
        const currentDeg = Math.round((normalizedY / (Math.PI * 2)) * 360);
        setRotationAngle(currentDeg);
      }

      renderer.render(scene, camera);
      frameIdRef.current = requestAnimationFrame(animate);
    };

    frameIdRef.current = requestAnimationFrame(animate);

    // Resize Handler
    const handleResize = () => {
      if (!container || !camera || !renderer) return;
      const newW = container.clientWidth;
      const newH = container.clientHeight;
      camera.aspect = newW / newH;
      camera.updateProjectionMatrix();
      renderer.setSize(newW, newH);
    };

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      if (frameIdRef.current) cancelAnimationFrame(frameIdRef.current);
      if (renderer.domElement && renderer.domElement.parentNode) {
        renderer.domElement.parentNode.removeChild(renderer.domElement);
      }
      renderer.dispose();
    };
  }, [isOpen, product, isAutoRotating, buildProductModel]);

  // Touch and Mouse Drag Handlers to spin 360° smoothly
  const handlePointerDown = (clientX: number) => {
    isDraggingRef.current = true;
    previousPointerXRef.current = clientX;
    velocityRef.current = 0;
    setHasInteracted(true);
    setIsAutoRotating(false);
  };

  const handlePointerMove = (clientX: number) => {
    if (!isDraggingRef.current || !productGroupRef.current) return;
    const deltaX = clientX - previousPointerXRef.current;
    previousPointerXRef.current = clientX;

    const rotationSpeed = 0.009;
    const step = deltaX * rotationSpeed;
    productGroupRef.current.rotation.y += step;
    velocityRef.current = step; // Store velocity for smooth fling
  };

  const handlePointerUp = () => {
    isDraggingRef.current = false;
  };

  // Rotate camera to specified degree angle
  const rotateToDegree = (deg: number) => {
    haptics.selection();
    setHasInteracted(true);
    setIsAutoRotating(false);
    velocityRef.current = 0;

    if (productGroupRef.current) {
      const targetRad = (deg / 360) * Math.PI * 2;
      productGroupRef.current.rotation.y = targetRad;
      setRotationAngle(deg);
    }
  };

  // Zoom control
  const handleZoom = (delta: number) => {
    haptics.light();
    setZoomLevel((prev) => {
      const next = Math.max(0.7, Math.min(1.8, prev + delta));
      if (cameraRef.current) {
        cameraRef.current.position.z = 4.2 / next;
      }
      return next;
    });
  };

  const resetView = () => {
    haptics.light();
    rotateToDegree(0);
    setZoomLevel(1);
    if (cameraRef.current) {
      cameraRef.current.position.set(0, 0.5, 4.2);
    }
    setIsAutoRotating(true);
    setActiveHotspot(null);
  };

  if (!isOpen || !product) return null;

  // Determine current orientation view label
  const getAngleLabel = (deg: number) => {
    if (deg >= 315 || deg < 45) return 'Front View (0°)';
    if (deg >= 45 && deg < 135) return 'Right Profile (90°)';
    if (deg >= 135 && deg < 225) return 'Rear View (180°)';
    return 'Left Profile (270°)';
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-0 sm:p-4 animate-in fade-in duration-200"
    >
      {/* Dark backdrop */}
      <div
        onClick={() => {
          haptics.light();
          onClose();
        }}
        className="fixed inset-0 bg-black/85 backdrop-blur-md"
      />

      {/* Main 360 Viewer Card (Flipkart Style) */}
      <div className="relative w-full max-w-[480px] h-[95vh] max-h-[820px] bg-slate-950 text-white rounded-t-3xl sm:rounded-3xl shadow-2xl flex flex-col z-10 overflow-hidden border border-slate-800">
        
        {/* Top Header with Flipkart Badge & Angle indicator */}
        <div className="flex items-center justify-between p-4 bg-slate-900/90 backdrop-blur-md border-b border-slate-800/80 z-20">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-[#2418D8] to-purple-600 text-white flex items-center justify-center shadow-lg shadow-indigo-500/20">
              <RotateCcw className="w-4 h-4 animate-spin-slow" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-black uppercase tracking-widest text-[#7B73FF] bg-[#2418D8]/20 px-2 py-0.5 rounded-md border border-[#2418D8]/30">
                  360° INTERACTIVE
                </span>
                <span className="text-[11px] font-bold text-slate-400">
                  {rotationAngle}°
                </span>
              </div>
              <h3 className="font-display font-black text-sm text-white truncate max-w-[240px] leading-tight mt-0.5">
                {product.name}
              </h3>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={resetView}
              title="Reset View"
              className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-full transition-colors cursor-pointer"
            >
              <RotateCcw className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={() => {
                haptics.light();
                onClose();
              }}
              title="Close 360 View"
              className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-full transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* View Mode Pill Switcher (3D Model vs Photo Sequence) */}
        <div className="flex items-center justify-between px-4 py-2 bg-slate-900/50 border-b border-slate-800/50 text-xs">
          <div className="flex items-center gap-1.5 text-slate-300 font-semibold text-[11px]">
            <Compass className="w-3.5 h-3.5 text-[#7B73FF]" />
            <span>{getAngleLabel(rotationAngle)}</span>
          </div>

          <div className="flex bg-slate-800 p-0.5 rounded-lg text-[10px] font-bold">
            <button
              type="button"
              onClick={() => setViewMode('3d')}
              className={`px-2.5 py-1 rounded-md transition-all cursor-pointer ${
                viewMode === '3d'
                  ? 'bg-[#2418D8] text-white shadow-xs'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              3D Studio Spin
            </button>
            <button
              type="button"
              onClick={() => setViewMode('frames')}
              className={`px-2.5 py-1 rounded-md transition-all cursor-pointer ${
                viewMode === 'frames'
                  ? 'bg-[#2418D8] text-white shadow-xs'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Photo Angles
            </button>
          </div>
        </div>

        {/* Interactive 3D / 360 Viewport Area */}
        <div
          className="relative flex-1 bg-gradient-to-b from-slate-900 via-slate-950 to-black overflow-hidden flex items-center justify-center select-none cursor-grab active:cursor-grabbing"
          onMouseDown={(e) => handlePointerDown(e.clientX)}
          onMouseMove={(e) => handlePointerMove(e.clientX)}
          onMouseUp={handlePointerUp}
          onMouseLeave={handlePointerUp}
          onTouchStart={(e) => {
            if (e.touches[0]) handlePointerDown(e.touches[0].clientX);
          }}
          onTouchMove={(e) => {
            if (e.touches[0]) handlePointerMove(e.touches[0].clientX);
          }}
          onTouchEnd={handlePointerUp}
        >
          {viewMode === '3d' ? (
            <div ref={mountRef} className="w-full h-full" />
          ) : (
            /* Photo Spin Angle simulation */
            <div className="relative w-full h-full flex items-center justify-center p-8">
              <div
                className="relative w-64 h-64 rounded-3xl overflow-hidden shadow-2xl transition-transform duration-150"
                style={{
                  transform: `rotateY(${rotationAngle}deg) scale(${zoomLevel})`,
                  perspective: '800px',
                }}
              >
                <img
                  src={product.image}
                  alt={product.name}
                  className="w-full h-full object-cover"
                />
                <div
                  className="absolute inset-0 bg-gradient-to-tr from-black/40 via-transparent to-white/20 pointer-events-none"
                  style={{
                    opacity: Math.abs(Math.sin((rotationAngle * Math.PI) / 180)),
                  }}
                />
              </div>
            </div>
          )}

          {/* Flipkart Interactive Gesture Guide Overlay (fades out after first drag) */}
          {!hasInteracted && (
            <div className="absolute inset-x-0 bottom-6 flex justify-center pointer-events-none animate-pulse">
              <div className="bg-slate-900/90 backdrop-blur-md border border-slate-700/80 px-4 py-2 rounded-full shadow-2xl flex items-center gap-2 text-xs font-bold text-white">
                <span className="text-base">↔</span>
                <span>Drag left or right to spin 360°</span>
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping ml-1" />
              </div>
            </div>
          )}

          {/* Floating Zoom and Auto-Spin Controls */}
          <div className="absolute top-4 right-4 flex flex-col gap-1.5 z-20">
            <button
              type="button"
              onClick={() => setIsAutoRotating(!isAutoRotating)}
              title={isAutoRotating ? 'Pause auto-spin' : 'Auto-spin 360°'}
              className={`w-9 h-9 rounded-xl backdrop-blur-md flex items-center justify-center transition-all cursor-pointer shadow-lg ${
                isAutoRotating
                  ? 'bg-[#2418D8] text-white ring-2 ring-indigo-400/50'
                  : 'bg-slate-800/80 hover:bg-slate-700 text-slate-300'
              }`}
            >
              {isAutoRotating ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4 ml-0.5" />}
            </button>

            <button
              type="button"
              onClick={() => handleZoom(0.2)}
              title="Zoom In"
              className="w-9 h-9 rounded-xl bg-slate-800/80 hover:bg-slate-700 backdrop-blur-md text-slate-300 flex items-center justify-center transition-all cursor-pointer shadow-lg"
            >
              <ZoomIn className="w-4 h-4" />
            </button>

            <button
              type="button"
              onClick={() => handleZoom(-0.2)}
              title="Zoom Out"
              className="w-9 h-9 rounded-xl bg-slate-800/80 hover:bg-slate-700 backdrop-blur-md text-slate-300 flex items-center justify-center transition-all cursor-pointer shadow-lg"
            >
              <ZoomOut className="w-4 h-4" />
            </button>
          </div>

          {/* Interactive Feature Hotspot Pills */}
          <div className="absolute top-4 left-4 flex flex-col gap-1.5 z-20">
            {hotspots.map((spot) => (
              <button
                key={spot.id}
                type="button"
                onClick={() => {
                  haptics.selection();
                  rotateToDegree(spot.angle);
                  setActiveHotspot(spot);
                }}
                className={`px-2.5 py-1 rounded-xl text-[10px] font-bold backdrop-blur-md border flex items-center gap-1.5 transition-all cursor-pointer shadow-md ${
                  activeHotspot?.id === spot.id || Math.abs(rotationAngle - spot.angle) < 25
                    ? 'bg-[#2418D8] border-indigo-400 text-white shadow-indigo-500/30'
                    : 'bg-slate-900/80 border-slate-700/80 text-slate-300 hover:text-white'
                }`}
              >
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                <span>{spot.title.split(' ')[0]} {spot.title.split(' ')[1] || ''}</span>
              </button>
            ))}
          </div>

          {/* Hotspot Detail Card Popup (when user clicks a feature highlight) */}
          {activeHotspot && (
            <div className="absolute bottom-4 left-4 right-4 bg-slate-900/95 backdrop-blur-md p-3.5 rounded-2xl border border-indigo-500/40 shadow-2xl z-20 animate-in slide-in-from-bottom-2">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <div className="flex items-center gap-1.5 text-[#7B73FF] text-[10px] font-black uppercase tracking-wider">
                    <Sparkles className="w-3 h-3" />
                    <span>Flipkart Product Highlight &bull; {activeHotspot.angle}° Angle</span>
                  </div>
                  <h4 className="font-bold text-xs text-white mt-0.5">
                    {activeHotspot.title}
                  </h4>
                  <p className="text-[11px] text-slate-300 mt-1 leading-relaxed">
                    {activeHotspot.description}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setActiveHotspot(null)}
                  className="p-1 text-slate-400 hover:text-white cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>

        {/* 360° Angle Scrubber Slider & Quick Snap Buttons */}
        <div className="p-3 bg-slate-900 border-t border-slate-800 space-y-2.5 z-20">
          {/* Degree Slider Bar */}
          <div className="space-y-1">
            <div className="flex items-center justify-between text-[10px] font-bold text-slate-400">
              <span>0° Front</span>
              <span>90° Right</span>
              <span>180° Back</span>
              <span>270° Left</span>
              <span>360°</span>
            </div>
            <input
              type="range"
              min="0"
              max="360"
              value={rotationAngle}
              onChange={(e) => rotateToDegree(Number(e.target.value))}
              className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-[#2418D8]"
            />
          </div>

          {/* Angle Preset Quick Buttons */}
          <div className="grid grid-cols-4 gap-1.5">
            {[
              { label: 'Front', deg: 0 },
              { label: 'Right (90°)', deg: 90 },
              { label: 'Back (180°)', deg: 180 },
              { label: 'Left (270°)', deg: 270 },
            ].map((preset) => (
              <button
                key={preset.deg}
                type="button"
                onClick={() => rotateToDegree(preset.deg)}
                className={`py-1.5 px-2 rounded-xl text-[10px] font-extrabold border transition-all cursor-pointer ${
                  Math.abs(rotationAngle - preset.deg) < 20
                    ? 'bg-[#2418D8] border-indigo-400 text-white shadow-xs'
                    : 'bg-slate-800/80 border-slate-700/60 text-slate-300 hover:bg-slate-800'
                }`}
              >
                {preset.label}
              </button>
            ))}
          </div>

          {/* Bottom Buy / Add to Cart Bar */}
          <div className="flex items-center justify-between gap-3 pt-2 border-t border-slate-800/80">
            <div>
              <div className="flex items-baseline gap-1.5">
                <span className="text-base font-black text-white">
                  ₹{product.price.toLocaleString('en-IN')}
                </span>
                <span className="text-[11px] text-slate-500 line-through">
                  ₹{product.originalPrice.toLocaleString('en-IN')}
                </span>
              </div>
              <span className="text-[10px] font-bold text-emerald-400">
                {product.discount} &bull; Free Delivery
              </span>
            </div>

            <div className="flex items-center gap-2">
              {onAddToCart && (
                <button
                  type="button"
                  onClick={() => {
                    haptics.success();
                    onAddToCart(product);
                  }}
                  className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs rounded-xl flex items-center gap-1.5 active:scale-95 transition-transform cursor-pointer"
                >
                  <ShoppingCart className="w-3.5 h-3.5" />
                  <span>Add to Cart</span>
                </button>
              )}

              {onBuyNow && (
                <button
                  type="button"
                  onClick={() => {
                    haptics.success();
                    onBuyNow(product);
                  }}
                  className="px-4 py-2 bg-gradient-to-r from-[#2418D8] to-purple-600 hover:from-[#1A10B3] hover:to-purple-700 text-white font-black text-xs rounded-xl shadow-lg shadow-indigo-500/25 flex items-center gap-1.5 active:scale-95 transition-transform cursor-pointer"
                >
                  <Zap className="w-3.5 h-3.5" />
                  <span>Buy Now</span>
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
