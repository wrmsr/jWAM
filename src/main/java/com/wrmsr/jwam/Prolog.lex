import java_cup.runtime.Symbol;

%% 

%class Lexer
%function nextToken
%type java_cup.runtime.Symbol
%public

%eofval{
	return new Symbol(sym.EOF);
%eofval}

Number = [0-9]+
EOL = \r | \n | \r\n
EmptySpace = {EOL} | [ \t\f]
Comment = ";" [^\n]* \n | "->" [^\n]* \n

% <letra pequena> <letra g/p | numero | '_'>*
Predicate = [:jletter:][:jletterdigit:]*[_]*

% <letra pequena> <letra g/p | numero | '_'>* | numero | "[]"
Constant = [:jletter:][:jletterdigit:]*[_]* | [0-9]+ | "[]"

% <letra pequena> <letra g/p | numero | '_'>* | "_"
Variavel = [:jletter:][:jletterdigit:]*[_]* | "_"

Comparator = ">" | "<" | "=" | "<=" | ">=" | "=/="

%%

/* keywords */
<YYINITIAL> "is"        { return new Symbol(sym.IS); }
<YYINITIAL> ":-"        { return new Symbol(sym.ATTRIB); }
<YYINITIAL> "!"         { return new Symbol(sym.CUT); }
<YYINITIAL> "not"         { return new Symbol(sym.NOT); }

<YYINITIAL> {
	"["             { return new Symbol (sym.RLP); }
	"]"             { return new Symbol (sym.RRP); }
	"."             { return new Symbol (sym.DOT); }
	","             { return new Symbol (sym.VIR); }
	"("             { return new Symbol (sym.LP); }
	")"             { return new Symbol (sym.RP); }
	{Comparator}    { return new Symbol (sym.COMPARATOR); }
	{Number}        { return (new Symbol(sym.INT_LIT, yytext())); }
	{Predicate}     { return (new Symbol(sym.PREDICATE, yytext())); }
	{Constant}      { return (new Symbol(sym.CONSTANT, yytext())); }
	{Variavel}      { return (new Symbol(sym.VARIAVEL, yytext())); }
	{EmptySpace}    { /* ignora os espacos em branco */ }
	{Comment}       { /* ignora os comentarios */  }
}
