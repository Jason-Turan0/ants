package hippo;
public enum SymmetryType {// there could be probably some more combinations, but let us stay with this ;) it's better to find a
	// subsymmetry than not find it at all so I will not cancel C just because there is more than 2 players
	S,	// (p) (x+v*rows/p,y+w*cols/p)
	R,	// (4) (v+y,w-x)
	C,	// (2) (v-x,w-y)
	D,	// (2) (y-v,x+v)
	B,	// (2) (v-y,v-x)
	Y,	// (2) (x,w-y)
	X,	// (2) (v-x,y)
	N;  // no symmetry
}