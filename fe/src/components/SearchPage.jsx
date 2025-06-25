import { BadgeDemo } from "@/components/BadgeList";
import { InputWithButton } from "@/components/InputWithButton";
import { TypographyH2 } from "@/components/Typographyh2";

export default function SearchPage() {
  return (
    <div className="flex flex-col justify-center h-screen items-center">
      <TypographyH2 />
      <InputWithButton />
      <BadgeDemo />
    </div>
  );
}
