// Icon generation for A3DCamera app
// Programatically draw the Android icon in safe area and output transparent png files
//
private static final int safeAreaSize = 384;
private static final int iconSize = 512;
private static final int iconOffset = iconSize/8;
private static final int safeAreaOffset = safeAreaSize/8;

void settings() {
  size(iconSize, iconSize);
}

void setup() {
  background(255); // white
}

void draw() {
  int strokeSize = 4;
  int x = (iconSize - safeAreaSize)/2;
  int y = x;
  PGraphics pg = createGraphics(iconSize, iconSize);
  pg.beginDraw();
  // draw safe area
  pg.background(#00FFFFFF); // all transparent
  //pg.stroke(color(255, 0, 0));
  //pg.rect(x-1, y-1, safeAreaSize+2, safeAreaSize+2);

  // draw camera outline
  pg.stroke(0);
  pg.strokeWeight(strokeSize);
  int camWidth = safeAreaSize;
  int camHeight = safeAreaSize-2*iconOffset;
  pg.fill(color(255, 255, 0));
  pg.rect(x, y+iconOffset, camWidth, camHeight, 64);
  pg.rect(x, y+iconOffset, camWidth, camHeight, 96);
  //pg.fill(224); // light gray
  pg.fill(192);
  //pg.fill(color(0,0,224)); //  blue shade
  pg.rect(x, y+iconOffset, camWidth, camHeight, 128);

  // draw lens
  int xLens1 = camWidth/4 +camWidth/32 + x;
  int yLens1 = camHeight/2 + y +iconOffset;
  int xLens2 = 6*safeAreaOffset -camWidth/32+ x;
  int yLens2 =  camHeight/2 + y +iconOffset;
  int lensSize = 2*safeAreaOffset + safeAreaOffset/2;
  pg.ellipse(xLens1, yLens1, lensSize, lensSize);
  pg.ellipse(xLens2, yLens2, lensSize, lensSize);

  // color lens
  pg.fill(color(0, 255, 255));
  pg.noStroke();
  pg.ellipse(xLens1, yLens1, lensSize-strokeSize, lensSize-strokeSize);
  pg.fill(color(255, 0, 0));
  pg.ellipse(xLens2, yLens2, lensSize-strokeSize, lensSize-strokeSize);
  pg.endDraw();
  // save 512x 5x2
  pg.save("tA3DCicon.png");

  background(255);
  image(pg, 0, 0);
  save("A3DCicon.png"); // not transparent
  noLoop();
}
