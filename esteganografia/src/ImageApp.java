import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.Vector;
import javax.imageio.ImageIO;

public class ImageApp   {
	public static void main(String[] args) {

		//carregando a imagem
		BufferedImage imgJPEG = loadImage("http://www.wildlifetrusts.org/sites/default/files/images/Planting%20along%20realigned%20River%20Chelt%20Gloucs%20WT,%20EA%20and%20landowner2%20comp.jpg");

		//texto de entrada
		String texto="Esteganografia eh a arte de ocultar uma informacao dentro da outra. \nTente encontrar essa mensagem na imagem, se puder... ";
		System.out.println("Texto Entrada:\n'"+texto+"'");

		//conversão da mensagem para binário
		String binaryText = stringToBinary(texto);
		System.out.println("Tamanho binário:\n"+binaryText.length());

		//criacao da imagem esteganografada
		System.out.println("Esteganografando...");
		BufferedImage esteganografada = esteganografa(binaryText, imgJPEG);

		
		System.out.println("Recuperando mensagem");
		//processo de recuperacao da mensagem
		String texto_out = recuperaMsg(esteganografada, binaryText.length());

		//Verificacao da entrada e saida
		if(binaryText.equals(texto_out)){
			System.out.println("Sucesso");
		}
		else{
			System.out.println("Algo deu errado...");
		}

		//conversão do binário recuperado para texto
		System.out.println("Texto Saida:\n'"+binToString(texto_out)+"'");

		//salvando imagens
		saveImage("Original", imgJPEG);
		saveImage("Esteganografada", esteganografada);

		System.out.println("fim");

	}

	//converte binário para string, usado para tornar mensagens legiveis
	//Códio retirado de: http://stackoverflow.com/questions/6222526/parsing-a-string-of-binary-into-text-characters 
	public static String binToString(String bin){
		StringBuilder b = new StringBuilder();
		while(bin.length()%8!=0){
			bin = "0"+bin;
		}
		int len = bin.length();
		System.out.println(bin);
		int i = 0;
		while (i + 8 <= len) {
			char c = convert(bin.substring(i, i+8));
			i+=8;
			b.append(c);
		}
		return b.toString();
	}

	private static char convert(String bs) {
		return (char)Integer.parseInt(bs, 2);
	}

	//converte uma string para codificacao em binario
	//retirado de: http://stackoverflow.com/questions/12310017/how-to-convert-a-byte-to-its-binary-string-representation
	public static String stringToBinary(String str) {
		byte[] bytes = str.getBytes();
		StringBuilder binaryStringBuilder = new StringBuilder();
		for(int i =0; i<bytes.length;i++){
			byte b = bytes[i];
			for(int j = 0; j < 8; j++)
				binaryStringBuilder.append(((0x80 >>> j) & b) == 0? '0':'1');
		}
		return binaryStringBuilder.toString();   
	}

	//cálculo de logaritmo base2
	//usado para computar a quantidade de bits necessarios para indexar uma coordenada
	public static Integer log2(int value) {
		return Integer.SIZE-Integer.numberOfLeadingZeros(value);
	}


	public static int getBits(int a, int b, int num){
		int tempValue = num << (31 - b);
		return tempValue >>> (31 - b + a);
	}
	
	public static int getBits(int a, int b, String num){
		String tmp="0";
		for(int i =a; i<=b;i++){
			tmp+= num.charAt(i);
		}
		return Integer.parseInt(tmp, 2);
	}

	//Codigo usado para esteganografar uma imagem.
	//recebe como parametro um texto e uma imagem
	public static BufferedImage esteganografa(String texto, BufferedImage img ) {
		//calculo da quantidade de bits para indexar as coordenadas
		int xBits = log2(img.getHeight());
		int yBits = log2(img.getWidth());

		String coordenadas = "";

		//separação dos bits de coordenada, foi escolhido o canal verde para ser alterado
		boolean end = true;
		//		//System.out.println("Separando coordenadas");
		for(int j=0;j<img.getHeight()&end;j++){
			for(int i=0;i<img.getWidth()&end;i++) {
				int pixelAtual = img.getRGB(i, j);
				int g = (int)((pixelAtual&0x0000FF00)>>>8); // componente verde
				coordenadas =coordenadas+ String.valueOf(getBits(1,7,g));
				//acaba a computação quando houver bits o suficiente para indexar todos os pares de bits do texto
				end = ((coordenadas.length())<=((xBits+yBits)*(texto.length()))/2);
			}
		}
				
		//separa coordenadas x e y;
		Vector<Integer> coordenadas_x = new Vector<>();
		Vector<Integer> coordenadas_y = new Vector<>();
		int index_Coord=coordenadas.length()-1;
		while(index_Coord>=0){
			
			int x_coord = getBits(index_Coord,index_Coord-xBits+1,coordenadas);
			index_Coord=index_Coord-xBits;
			int y_coord = getBits(index_Coord,index_Coord-yBits+1,coordenadas);
			index_Coord=index_Coord-yBits;
			
			coordenadas_x.addElement(x_coord);
			coordenadas_y.addElement(y_coord);
		}

		//cria a imagem que será modificada
		BufferedImage copia= new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = copia.getRaster();

		int index_Text =0;
		int count_pos=0;
		end = true;
		for(int j=0;j<img.getHeight();j++){
			for(int i=0;i<img.getWidth();i++) {

				//lê todos os componentes do pixel
				int pixelAtual = img.getRGB(i, j);
				int r = (int)((pixelAtual&0x00FF0000)>>>16); // componente vermelho
				int g = (int)((pixelAtual&0x0000FF00)>>>8); // componente verde
				int b = (int)(pixelAtual&0x000000FF); //componente azul

				//enquanto houver texto para esteganografar, altera o canal verde.
				if(end){
					g = (g>>1)<<1;//zerar o bit menos significativo;
					//seleciona o pixel alvo com base nas coordenadas encontradas
					int pixelAlvo = img.getRGB((coordenadas_x.get(count_pos)%img.getWidth()), (coordenadas_y.get(count_pos)%img.getHeight()));

					//leitura do canal verde do pixel alvo
					int g2 = (int)((pixelAlvo&0x0000FF00)>>>8);

					//selecao do bit do texto e o bit base do pixel alvo
					int b1 = Character.getNumericValue((texto.charAt(index_Text)));
					int t1 = getBits(7-i%2, 7-i%2, g2);
					//calculo do novo bit do pixel
					int a1 = b1^t1;
					//alteração do canal verde
					g += a1 ;

					//ajuste dos valores que índices
					index_Text++;
					count_pos+=i%2;
					end=!(index_Text>=texto.length());
				}

				//criação do pixel na nova imagem
				raster.setSample(i,j,0,r);
				raster.setSample(i,j,1,g);
				raster.setSample(i,j,2,b);

			}
		}

				//System.out.println("gerando imagem");
		ColorModel colorModel = copia.getColorModel();
		copia = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
		return copia;

	}

	public static String recuperaMsg(BufferedImage img, int tamanho) {
		//System.out.println("Recuperando Msg........");
		int xBits = log2(img.getHeight());
		int yBits = log2(img.getWidth());

		BigInteger coordenadas = new BigInteger("0");
		Vector<Integer> ajuste = new Vector<>();
		//separação dos bits de coordenada e bits de ajuste.
		boolean end = true;
		//		//System.out.println("Separando coordenadas e ajuste");
		for(int j=0;j<img.getHeight()&end;j++){
			for(int i=0;i<img.getWidth()&end;i++) {
				int pixelAtual = img.getRGB(i, j);
				int g = (int)((pixelAtual&0x0000FF00)>>>8); // componente verde
				//System.out.println(g);
				coordenadas = (coordenadas.shiftLeft(7)).add(new BigInteger(String.valueOf(getBits(1,7,g))));
				end = ((coordenadas.bitLength())<=((xBits+yBits)*(tamanho+18))/2);
				ajuste.add(getBits(0, 0, g));
			}
		}
		//		//System.out.println(coordenadas.toString());

		//		//System.out.println("montando coordenadas x e y");
		//separa coordenadas x e y;
		Vector<Integer> coordenadas_x = new Vector<>();
		Vector<Integer> coordenadas_y = new Vector<>();
		int index_Coord = coordenadas.bitLength();
		while(index_Coord>=0){
			
			int x_coord = getBits(index_Coord,index_Coord-xBits+1,coordenadas.toString());
			index_Coord=index_Coord-xBits;
			int y_coord = getBits(index_Coord,index_Coord-yBits+1,coordenadas.toString());
			index_Coord=index_Coord-yBits;

			coordenadas_x.addElement(x_coord);
			coordenadas_y.addElement(y_coord);			
		}

		String texto ="";
		//		//System.out.println("recuperando texto");
		int index_Text =0;
		int count_pos=0;
		end = true;
		for(int j=0;j<img.getHeight();j++){
			for(int i=0;i<img.getWidth();i++) {
				if(end){
					int pixelAlvo = img.getRGB((coordenadas_x.get(count_pos)%img.getWidth()), (coordenadas_y.get(count_pos)%img.getHeight()));
					int g2 = (int)((pixelAlvo&0x0000FF00)>>>8);

					//recuperacao de um bit do texto
					texto+= getBits(7-i%2, 7-i%2, g2)^ajuste.get(index_Text);

					index_Text++;
					count_pos+=i%2;
					end=!(index_Text>=tamanho);
				}
			}
		}
		return texto;
	}
	
	// Leitura da imagem
	public static BufferedImage loadImage(String surl) {  
		BufferedImage bimg = null;  
		try {  
			URL url = new URL(surl);
			bimg = ImageIO.read(url);  
			//bimg = ImageIO.read(new File("D:/Temp/mundo.jpg"));
		} catch (Exception e) {  
			e.printStackTrace();  
		}  
		return bimg;  
	}  

	//salva imagem
	public static void saveImage(String name, BufferedImage img) {
		try {
			// retrieve image
			BufferedImage bi = img;
			File outputfile = new File(name+".png");
			ImageIO.write(bi, "png", outputfile);
		} catch (IOException e) {
			System.out.println("shit");
		}
	}
}





//	public static BufferedImage diferenca(BufferedImage img1, BufferedImage img2 ) {
//		int width1 = img1.getWidth(); // Change - getWidth() and getHeight() for BufferedImage
//		int width2 = img2.getWidth(); // take no arguments
//		int height1 = img1.getHeight();
//		int height2 = img2.getHeight();
//		if ((width1 != width2) || (height1 != height2)) {
//			System.err.println("Error: Images dimensions mismatch");
//			System.exit(1);
//		}
//
//		// NEW - Create output Buffered image of type RGB
//		BufferedImage outImg = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_RGB);
//
//		// Modified - Changed to int as pixels are ints
//		int diff;
//		int result; // Stores output pixel
//		for (int i = 0; i < height1; i++) {
//			for (int j = 0; j < width1; j++) {
//				int rgb1 = img1.getRGB(j, i);
//				int rgb2 = img2.getRGB(j, i);
//				int r1 = (rgb1 >> 16) & 0xff;
//				int g1 = (rgb1 >> 8) & 0xff;
//				int b1 = (rgb1) & 0xff;
//				int r2 = (rgb2 >> 16) & 0xff;
//				int g2 = (rgb2 >> 8) & 0xff;
//				int b2 = (rgb2) & 0xff;
//				//				diff = Math.abs(r1 - r2); // Change
//				diff = Math.abs(g1 - g2);
//				//				diff += Math.abs(b1 - b2);
//				; // Change - Ensure result is between 0 - 255
//				// Make the difference image gray scale
//				// The RGB components are all the same
//				//				result = (diff << 16) | (diff << 8) | diff;
//				outImg.setRGB(j, i, diff); // Set result
//			}
//		}
// Now return
//	return outImg;