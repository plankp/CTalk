package com.ymcmp.ctalk.compiler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

public class App {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.net.URISyntaxException
     */
    public static void main(String[] args) throws IOException, URISyntaxException {
        final URL res = App.class.getResource("hello.ct");
        final CharStream inp = CharStreams.fromStream(res.openStream());
        final GrammarLexer lex = new GrammarLexer(inp);
        final TokenStream toks = new CommonTokenStream(lex);
        final GrammarParser parser = new GrammarParser(toks);
        System.out.println(new Translator(res.toURI()).generate(parser.program()));
    }
}
